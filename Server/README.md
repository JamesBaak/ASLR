## Running the Server:
1. Navigate to the Server folder
2. The addresses and ports of the server and machine learning Pi's can be modified in the `udp_server.py` file. Maybe move this to a file later to read in address values.
3. Run the UDP server using Python > 3
```
python udp_server.py
```
The server should connect to the database with the Database folder. If it does not exist it will be created, but will not function as the tables do not exist. If the server is unable to connect to the Machine Learning (ML) Pi or if there is an issue, the server will return an error to sample requestor indicating the issue with the ML Pi.

## Interfacing with the server:
The server takes UDP requests with a string representing a JSON in the content. The JSON should be encoded as a string with UTF-8.
The JSON has the keys `type` and `payload`. The keys/fields are required to communicate with the server, otherwise an error message will be returned.
Table of supported types and their respective payloads:

Type | Payload | Response |
---- | ------- | -------- |
sample | empty string to get prediction from ML PI or an int representing one of the gestures | { "type": "prediction", "payload": int }
load | The username of the user's data that the machine learning algorithm should train with or empty string to load all records  | { type: "ack", payload: "" }
create_user | [json: { username: string, saltValue: string, password: string, developer: int }] | { type: "ack", payload: "" }
get_user | The username of the user's record to return | { type: "user", payload: { "username" : string, "salt"     : string, "password" : string, "developer": int }}
