import serial_buffer
import socket
import json

# Note: Machine learning algorithm from SensIR specs in their paper at: https://dl.acm.org/citation.cfm?doid=3126594.3126604
import numpy as np
from sklearn.multiclass import OneVsRestClassifier
from sklearn.neural_network import MLPClassifier
from sklearn.preprocessing import StandardScaler
from sklearn import model_selection
from sklearn.preprocessing import MultiLabelBinarizer

PORT = 9999
HOST_ADDRESS = "localhost"
ACK   = { "type": "ack", "payload": "" }
ERROR = { "type": "error", "payload": "" }

#Setup the ML model
clf = OneVsRestClassifier(MLPClassifier(solver='lbfgs', alpha=0.05, hidden_layer_sizes=(24,), random_state=1))
scaler = StandardScaler()
mlb = MultiLabelBinarizer()
labels = []
features = []

def __constructJSON__(self, msgType, msg):
        """
        Construct an error json for UDP response
        msg [string] - The payload of the error message
        """
        m_json = msgType.copy()
        m_json["payload"] = msg
        return m_json

def handleSample(socket, address, payload, buffer):
    """
    If payload is 0 then the ML algorithm does not need to retrain.
    Greater than 0 indicts a gesture and the known class will be added 
    to the training data set to train model again before prediction.
    Finally, return the response back to server.
    socket - socket of request
    address - response address
    payload [int] - 
    """
    print("Training model and prediction commencing...")
    in_vector = scaler.transform(np.array(buffer))

    if payload > 0:
        print("No gesture selected, skipping training...")
    else:
        labels.append(payload)
        features.append(in_vector)
        train_model()
        
    #Prediction
    pred = clf.predict(in_vector)
    result = (pred, payload, in_vector)

def train_model():
    # Do this so we can avoid overfitting due to our small data set
    X_train, X_test, y_train, y_test = model_selection.train_test_split(np.array(features), np.array(labels), test_size=0.3, random_state=42)
    clf.fit(X_train,y_train)
    acccuracy = clf.score(X_test, y_test)
    print("Model Trained...")
    print("Accuracy: {}".format(acccuracy))


def main():
    # Host setup
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    host_address = (HOST_ADDRESS, PORT)
    sock.bind(host_address)
    # Buffer setup
    sb = serial_buffer.SerialBuffer('/dev/ttyACM0',9600)
    sb.debug = True
    sb.start()

    # Loop to wait for sample requests to then return the sample data to save.
    while True:
        print("Waiting for sample request...")
        buf, address = sock.recvfrom(PORT)

        if not len(buf):
            break

        request = json.loads(buf.decode("utf-8"))

         # Check to see if type and payload are in the json message request to the server
        if not "type" in request:
            socket.sendto(
                bytes(json.dumps(__constructJSON__(ERROR, "Missing type field...")), "utf-8"),
                address
            )
        elif not "payload" in request:
            socket.sendto(
                bytes(json.dumps(__constructJSON__(ERROR, "Missing payload field...")), "utf-8"),
                address
            )
        # Valid request structure, so handle it if valid type, else reply with error message
        else:
             # Process request
            if request["type"] == "sample": # Send request to ML PI to sample and return response
                handleSample(socket, address, request["payload"], sb.readBuffer())
            else: # Request type not specified
                socket.sendto(
                    bytes(json.dumps(__constructJSON__(ERROR, "Unknown type field...")), "utf-8"),
                    address
                )

#     # Send back ack
#     data = "ACK: " + buf.decode("utf-8")
#     s.sendto(data.encode('utf-8'), address)

if __name__ == "__main__":
    main()