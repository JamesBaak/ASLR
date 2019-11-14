##Running the Server:
1. Navigate to the Server folder
2. The addresses and ports of the server and machine learning Pi's can be modified in the `udp_server.py` file. Maybe move this to a file later to read in address values.
3. Run the UDP server using Python > 3
```
python udp_server.py
```
The server should connect to the database with the Database folder. If it does not exist it will be created, but will not function as the tables do not exist.

##Interfacing with the server:
The server takes UDP requests with a string representing a JSON in the content. The JSON should be encoded as a string with UTF-8.
The JSON has the keys `type` and `payload`. The keys/fields are required to communicate with the server, otherwise an error message will be returned.
Table of supported types and their respective payloads:
Type | Payload
---- | -------
sample | empty string to get prediction from ML PI or an int representing one of the gestures
save | [json: { "prediction": string, "class": string, "input": int[196] }]
load | The username of the user's data that the machine learning algorithm should train with 
create_user | [json: { username: string, saltValue: string, password: string, developer: int }]
get_user | The username of the user's record to return
