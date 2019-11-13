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
                bytes(json.dumps(self.constructError("Missing type field...")), "utf-8"),
                self.client_address
            )
        elif not "payload" in request:
            socket.sendto(
                bytes(json.dumps(self.constructError("Missing payload field...")), "utf-8"),
                self.client_address
            )
        else:
            socket.sendto(bytes(json.dumps(ACK), "utf-8"), self.client_address)
            # Process request

    def constructError(self, msg):
        error0 = ERROR.copy()
        error0["payload"] = msg
        return error0

if __name__ == "__main__":
    HOST, PORT = "", 9999
    DB_LOC = "./Database/aslr_database.db"
    with MyUDPServer((HOST, PORT), MyUDPHandler,  True, "") as server:
        server.serve_forever()