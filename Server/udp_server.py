import socketserver
import socket
import json
import copy
import platform
from Database.sql_database import Database
from socket import error as SocketError


# https://docs.python.org/3/library/socketserver.html
# https://docs.python.org/3/library/socketserver.html#socketserver-udpserver-example

# Configurable parameters of the server
HOST, PORT = "", 9999
ML_HOST, ML_PORT = "169.254.85.238", 1024 # The location of the ML Pi

# JSON msg types for construction of responses
ERROR   = { "type": "error", "payload": "" }
ACK     = { "type": "ack", "payload": "" }
USER    = { "type": "user", "payload": {} }
SAMPLE  = { "type": "sample", "payload": 0 }
PRED    = { "type": "prediction", "payload": 0 }
RECORDS = { "type": "records", "payload": { "records": [], "remaining": 0 }}
MAX_REC = 40

class MyUDPServer(socketserver.UDPServer):
    
    def __init__(self, server_address, RequestHandlerClass, bind_and_activate, db_file, ml_addr):
        super().__init__(server_address, RequestHandlerClass, bind_and_activate)
        self.database = Database(db_file)
        self.ml_addr = ml_addr
        self.ml_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.ml_sock.settimeout(15)
        self.current_userId = 0

class MyUDPHandler(socketserver.BaseRequestHandler):

    def handle(self):
        request = json.loads(self.request[0].decode('utf-8'))
        socket = self.request[1]
        print("Request from {}:".format(self.client_address[0]))
        print(request)

        # Check to see if type and payload are in the json message request to the server
        if not "type" in request:
            socket.sendto(
                bytes(json.dumps(self.__constructJSON__(ERROR, "Missing type field...")), "utf-8"),
                self.client_address
            )
        elif not "payload" in request:
            socket.sendto(
                bytes(json.dumps(self.__constructJSON__(ERROR, "Missing payload field...")), "utf-8"),
                self.client_address
            )
        # Valid request structure, so handle it if valid type, else reply with error message
        else:
            # Process request
            if request["type"] == "sample": # Send request to ML PI to sample and return response
                self.__handleSample__(socket, request["payload"])
            elif request["type"] == "save": # From the ML PI with the ML result record
                self.__handleSave__(socket, request["payload"])
            elif request["type"] == "load": # Load the users data into the Machine learning PI
                self.__handleLoad__(socket, request["payload"])
            elif request["type"] == "create_user": # Create the new user in the database
                self.__handleCreateUser__(socket, request["payload"])
            elif request["type"] == "get_user": # Get a user by username and return to sender
                self.__handleGetUser__(socket, request["payload"])
            else: # Request type not specified
                socket.sendto(
                    bytes(json.dumps(self.__constructJSON__(ERROR, "Unknown type field...")), "utf-8"),
                    self.client_address
                )

    def __constructJSON__(self, msgType, msg):
        """
        Construct an error json for UDP response
        msg [string] - The payload of the error message
        """
        m_json = msgType.copy()
        m_json["payload"] = msg
        return m_json

    def __handleSample__(self, socket, payload):
        """
        Sample the bracelet and get ML prediction
        payload [string] - Empty string to get prediction, and int representing gesture
        """
        self.__sendAck__(socket)
        response = self.__attemptSample__(payload)
        data = "Cannot connect to the ML Pi"

        if (response != None and response['type'] == "save"):
            data = response["payload"]
            socket.sendto(
                bytes(json.dumps(self.__constructJSON__(PRED, data["prediction"])), "utf-8"),
                self.client_address
            )

            # Save the event sample in the database
            # event: (userId: int,data: json string, pred: int, class: int)
            self.server.database.insert_event((
                self.server.current_userId,
                json.dumps({ "data": data["input"] }),
                data["prediction"],
                data["class"] # class is a keyword in python
            ))
        else:
            socket.sendto(
                bytes(json.dumps(self.__constructJSON__(ERROR, "ML PI Error...: {}".format(data))), "utf-8"),
                self.client_address
            )


    def __attemptSample__(self, payload):
        # Can access the ML PI address and socket through self.server.ml_addr
        # Create request and forward payload
        request = SAMPLE.copy()
        if payload == "":
            request["payload"] = 0
        else:
            request["payload"] = payload
        response = None

        # Forward sample request
        self.server.ml_sock.sendto(bytes(json.dumps(request), "utf-8"), self.server.ml_addr)

        # Wait for 'save' response from ML algorithm
        try:
            response = json.loads(str(self.server.ml_sock.recv(2048), "utf-8"))
        except SocketError as e:
            pass

        return response

    def __handleSave__(self, socket, payload):
        """
        Should write the ML event result to the MLResults database 
        payload [json: { "prediction": string, "class": string, "input": int[196] }]
        """
        # Can access the database object through self.server.database
        print(payload)
               
    def __handleLoad__(self, socket, payload):
        """
        Send the user's data records to the ML PI for training
        payload [string] - The username of the user to load the ML PI with. If blank string (""),
                        then load all data records.
        """
        # Local function to transform records from MLResults to
        # JSON object
        def transform(record):
            return {
                "pred": record[2],
                "class": record[3],
                "input": record[1]
            }

        self.__sendAck__(socket)

        if (payload == ""):
            results = self.server.database.get_all_events()
        else:
            results = self.server.database.get_user_events(payload)

        if (results != None):
            # Transform records
            records = map(transform, results)
            

            # Record size should be around 196 * (8 float size) = 1568 bytes + other details ~ 1600 bytes
            # Max UDP packet can be 65535 - 20 = 65535 -> - 20 / 1600 ~ 40.9
            # Can send about 40 records over in one UDP packet
            rec_len = len(records)
            div = rec_len // MAX_REC
            remainder = rec_len % MAX_REC
            request = copy.deepcopy(RECORDS) # Reuse one record packet

            if (div == 0): # We can fit all records in one packet
                request["payload"]["records"] = records
                self.server.ml_sock.sendto(bytes(json.dumps(request), "utf-8"), self.server.ml_addr)
            else:
                rec_start = 0
                rec_end   = rec_len
                # Have to send remainder packet after div count
                for i in range(div):
                    rec_start = i * 40
                    rec_end   = (i + 1) * 40
                    remaining = div - i
                    if (remainder == 0): remaining - 1
                    request["payload"]["remaining"] = div - i - 1
                    request["payload"]["records"] = records[rec_start:rec_end]
                    self.server.ml_sock.sendto(bytes(json.dumps(request), "utf-8"), self.server.ml_addr)
                if (remainder > 0):
                    request["payload"]["remaining"] = 0
                    request["payload"]["records"] = records[rec_end + 1:rec_len]
                    self.server.ml_sock.sendto(bytes(json.dumps(request), "utf-8"), self.server.ml_addr)
        else:
            print("No saved events for user {}".format(payload))


    def __handleCreateUser__(self, socket, payload):
        """
        Create the new user in the database from the payload
        payload [json: { username: string, saltValue: string, password: string, developer: int }]
        """
        self.__sendAck__(socket)
        next_id = self.server.database.get_user_len()
        self.server.database.create_user((
            next_id,
            payload["username"],
            payload["saltValue"],
            payload["password"],
            payload["developer"]
        ))

    def __handleGetUser__(self, socket, payload):
        """
        Get the user by username and return the user to the requestor
        payload [string] - The username of the user to return
        """
        print(payload)
        user_t = self.server.database.get_user(payload) # Tuple of the user
        if user_t == None:
            socket.sendto(
                bytes(json.dumps(self.__constructJSON__(ERROR, "User does not exist...")), "utf-8"),
                self.client_address
            )
        else:
            user_json = {
                "username" : user_t[1],
                "salt"     : user_t[2],
                "password" : user_t[3],
                "developer": user_t[4]
            }
            socket.sendto(
                bytes(json.dumps(self.__constructJSON__(USER, user_json)), "utf-8"),
                self.client_address
            )

    def __sendAck__(self, socket):
        socket.sendto(bytes(json.dumps(ACK), "utf-8"), self.client_address)

if __name__ == "__main__":
    DB_LOC = None
    os_name = platform.system()
    if os_name == "Windows":
        DB_LOC = r".\Database\aslr_database.db"
    else:
        DB_LOC = r"./Database/aslr_database.db"
    server = MyUDPServer((HOST, PORT), MyUDPHandler, True, DB_LOC, (ML_HOST, ML_PORT))
    server.serve_forever()
