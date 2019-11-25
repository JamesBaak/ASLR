import udp_server
import socket
import json
import unittest
from unittest import mock

TEST_USER = "test_user"
TEST_SALT = "FFFF"
TEST_PASS = "password"
TEST_DEV  = 1

# Ensure to run this test from the Server folder of the ASLR project directory
# to access the mock_database
class TestServerCommunication(unittest.TestCase):

    HOST, PORT = "localhost", 9999
    ML_HOST, ML_PORT = "localhost", 9999
    DB_LOC = r"./Database/mock_database.db"
    server = None
    sock = None

    # send a request over localhost to the server
    # Return the response
    def sendRequest(self, request):

        TestServerCommunication.sock.sendto(
            bytes(json.dumps(request), "utf-8"),
            (TestServerCommunication.HOST, TestServerCommunication.PORT)
        )
        TestServerCommunication.server.handle_request()
        received = json.loads(str(TestServerCommunication.sock.recv(2048), "utf-8"))
        return received

    def receiveResponse(self):
        return json.loads(str(TestServerCommunication.sock.recv(2048), "utf-8"))

    def sendValidRequest(self, typeField, payloadField):
        request = {
            "type": typeField,
            "payload": payloadField
        }
        return self.sendRequest(request)

    def sendSampleRequest(self, gesture):
        return self.sendValidRequest("sample", gesture)

    @classmethod
    def setUpClass(cls):
        # SOCK_DGRAM is the socket type to use for UDP sockets
        TestServerCommunication.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        # Start up the server
        TestServerCommunication.server = udp_server.MyUDPServer(
            (TestServerCommunication.HOST, TestServerCommunication.PORT), 
            udp_server.MyUDPHandler,
            True,
            TestServerCommunication.DB_LOC,
            (TestServerCommunication.ML_HOST, TestServerCommunication.ML_PORT)
        )

        # Change the ML Pi socket to a mock one to run tests without it
        TestServerCommunication.server.ml_sock = mock.Mock()
        TestServerCommunication.server.ml_sock.sendto.return_value = True
        TestServerCommunication.server.ml_sock.recv.return_value = bytes(
            json.dumps({ 
                'type': "save",
                'payload': {
                    "prediction": 2,
                    "class": 2,
                    "input": []
                }
            }), 'utf-8')

    @classmethod
    def tearDownClass(cls):
        TestServerCommunication.server.ml_sock.close()
        TestServerCommunication.sock.close()

    """
    Test that requests to the server without the JSON key type will return a response
    with an error stating that the type is missing
    """
    def test_type(self):
        # Create and send request and receive response from server.
        # Check that the return type was error
        # Check that the return message contained the word type, to indicate type error
        request = {}
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('type', result["payload"])
        
        request = { 'payload': "" }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('type', result["payload"])

        request = { 'payload': {} }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('type', result["payload"])

    """
    Ensure that the payload is needed by the server to process any request
    """
    def test_payload(self):
        # Create and send request and receive response from server.
        # Check that the return type was error
        # Check that the return message contained the word payload, to indicate payload error
        request = { 'type': 'get_user' }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('payload', result["payload"])

        request = { 'type': 'create_user' }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('payload', result["payload"])

        request = { 'type': 'load' }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('payload', result["payload"])

        request = { 'type': 'sample' }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('payload', result["payload"])

    """
    Ensure that the server doesn't process unknown type requests and indicates this
    to the sender.
    """
    def test_invalid_type(self):
        # Create and send request and receive response from server.
        # Check that the return type was error
        # Check that the return message contained the word type, to indicate type error
        request = { 'type': '', 'payload': "" }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('type', result["payload"])

        request = { 'type': 'test', 'payload': "" }
        result = self.sendRequest(request)
        self.assertEqual(result["type"], "error", 'Should be of type error response')
        self.assertIn('type', result["payload"])

    """
    Ensure that a user can be retrieved by the requester from the database.
    Should return the user as the test user exists in the mock database
    """
    def test_get_user(self):
        result = self.sendValidRequest('get_user', TEST_USER)
        payload = result['payload']
        self.assertEqual(result['type'], "user")
        self.assertEqual(payload['username'], TEST_USER)
        self.assertEqual(payload['salt'], TEST_SALT)
        self.assertEqual(payload['password'], TEST_PASS)
        self.assertEqual(payload['developer'], TEST_DEV)

    """
    Test that retrieving a user that does not exist returns an error 
    indicating that the user does not exist.
    """
    def test_get_invalid_user(self):
        result = self.sendValidRequest('get_user', "nobody")
        self.assertEqual(result['type'], "error")
        self.assertIn('exist', result['payload'])

        result = self.sendValidRequest('get_user', 1)
        self.assertEqual(result['type'], "error")
        self.assertIn('exist', result['payload'])

    """
    Test that the server can handle receiving a user with empty string.
    """
    def test_get_no_user(self):
        result = self.sendValidRequest('get_user', "")
        self.assertEqual(result['type'], "error")
        self.assertIn('exist', result['payload'])
    
    """
    Test the bracelet sampling of the server with connection to the mock
    machine learning Pi. The mock object should return the prediction for
    a valid sample request.
    """
    def test_send_sample(self):
        result = self.sendSampleRequest(2)
        self.assertEqual(result['type'], "ack")
        result = self.receiveResponse()
        self.assertEqual(result['type'], "prediction")
        self.assertEqual(result['payload'], 2)

    def test_load_records(self):
        # Load all records test
        result = self.sendValidRequest("load", "")
        self.assertEqual(result['type'], "ack")

        # Load user's records test
        result = self.sendValidRequest("load", TEST_USER)
        self.assertEqual(result['type'], "ack")

if __name__ == '__main__':
    unittest.main()
