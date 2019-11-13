import socketserver
import json
from Database.sql_database import Database


# https://docs.python.org/3/library/socketserver.html
# https://docs.python.org/3/library/socketserver.html#socketserver-udpserver-example

ERROR = { "type": "error", "payload": "" }
ACK   = { "type": "ack", "payload": "" }

class MyUDPServer(socketserver.UDPServer):
    
    def __init__(self, server_address, RequestHandlerClass, bind_and_activate, db_file):
        super().__init__(server_address, RequestHandlerClass, bind_and_activate)
        self.database = Database(db_file)

class MyUDPHandler(socketserver.BaseRequestHandler):

    def handle(self):
        request = json.loads(self.request[0].strip())
        socket = self.request[1]
        print("Request from {}:".format(self.client_address[0]))
        print(request)

        if not "type" in request:
            socket.sendto(
                bytes(json.dumps(self.__constructError__("Missing type field...")), "utf-8"),
                self.client_address
            )
        elif not "payload" in request:
            socket.sendto(
                bytes(json.dumps(self.__constructError__("Missing payload field...")), "utf-8"),
                self.client_address
            )
        else:
            # Process request
            if request["type"] == "sample": # Send request to ML PI to sample and return response
                self.__handleSample__(socket, request["payload"])
            elif request["type"] == "save": # From the ML PI with the ML result record
                self.__handleSave__(socket, request["payload"])
            else: # Request type not specified
                socket.sendto(
                    bytes(json.dumps(self.__constructError__("Unknown type field...")), "utf-8"),
                    self.client_address
                )

    def __constructError__(self, msg):
        error0 = ERROR.copy()
        error0["payload"] = msg
        return error0

    def __handleSample__(self, socket, payload):
        print(payload)

    def __handleSave__(self, socket, payload):
        # Can access the database object through self.server.database
        print(payload)

if __name__ == "__main__":
    HOST, PORT = "", 9999
    DB_LOC = r".\Database\aslr_database.db" # For windows. The slashes may have to be changed for linux
    with MyUDPServer((HOST, PORT), MyUDPHandler, True, DB_LOC) as server:
        server.serve_forever()