import socketserver
import json
from Database.sql_database import Database


# https://docs.python.org/3/library/socketserver.html
# https://docs.python.org/3/library/socketserver.html#socketserver-udpserver-example

# JSON msg types for construction of responses
ERROR = { "type": "error", "payload": "" }
ACK   = { "type": "ack", "payload": "" }
SIGN  = { "type": "sign", "payload": "" }
USER  = { "type": "user", "payload": {} }

class MyUDPServer(socketserver.UDPServer):
    
    def __init__(self, server_address, RequestHandlerClass, bind_and_activate, db_file, ml_addr):
        super().__init__(server_address, RequestHandlerClass, bind_and_activate)
        self.database = Database(db_file)
        self.ml_addr = ml_addr

class MyUDPHandler(socketserver.BaseRequestHandler):

    def handle(self):
        request = json.loads(self.request[0].strip())
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
        # Can access the ML PI address through self.server.ml_addr
        self.__sendAck__(socket)
        print(payload)

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
        self.__sendAck__(socket)
        print(payload)

    def __handleCreateUser__(self, socket, payload):
        """
        Create the new user in the database from the payload
        payload [json: { username: string, saltValue: string, password: string, developer: int }]
        """
        self.__sendAck__(socket)
        next_id = self.server.database.get_user_len()
        self.server.database.create_user((
            next_id,
            payload.username,
            payload.saltValue,
            payload.password,
            payload.developer
        ))

    def __handleGetUser__(self, socket, payload):
        """
        Get the user by username and return the user to the requestor
        payload [string] - The username of the user to return
        """
        print(payload)
        user_t = self.server.database.get_user(payload) # Tuple of the user
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
    HOST, PORT = "", 9999
    ML_HOST, ML_PORT = "localhost", 1024
    DB_LOC = r".\Database\aslr_database.db" # For windows. The slashes may have to be changed for linux
                                            # We can also check OS and change DB_LOC var respectivly
    with MyUDPServer((HOST, PORT), MyUDPHandler, True, DB_LOC, (ML_HOST, ML_PORT)) as server:
        server.serve_forever()