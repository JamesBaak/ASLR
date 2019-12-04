package com.example.aslrapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*
    Return value for the login function
    Indicates the if the login was successful and if the login account belongs  to a developer
 */
final class LoginResult {
    private final Boolean result;
    private final Boolean developer;

    public LoginResult(Boolean result, Boolean developer) {
        this.result = result;
        this.developer = developer;
    }

    /*
        Returns the a value indicating if the login was successful
     */
    public Boolean getResult() {
        return result;
    }

    /*
        Returns a value indicating if the logged in account belongs to a developer
     */
    public Boolean getDeveloper() {
        return developer;
    }
}


/*
    The class for the login activity
    Controls the login functionality of the application
 */
public class LoginActivity extends AppCompatActivity {

    // a tag for error logging
    private final String TAG = "LoginActivity";
    // charset for encoding and decoding byte arrays to Strings
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    // indicates if the application is in a timeout state
    protected Boolean lockFlag = false;
    // the timeout period for the application
    private final int SLEEPTIME = 5;

    // login button
    private Button mLoginButton;
    // edit text views for the username and password
    private EditText mUsername;
    private EditText mPassword;
    // text view showing the results of the login
    private TextView mResultView;

    // Port and address information for sending requests to the server
    private int PORT = 9999;
    private InetAddress ADDR;
    private final static int PACKETSIZE = 1024;

    private DatagramSocket socket = null;

    // number if unsuccessful login attempts
    private int numTries = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // activity setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // turns off the default policy of having no network operations in the main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // TODO for testing purposes -> update when connected to Pi
        try {
            ADDR = InetAddress.getByName("10.0.2.2");
        } catch (UnknownHostException e){
            Log.e(TAG, "Unknown host exception when creating address!");
            e.printStackTrace();
        }

        // create socket to send
        Boolean createResult = createSocket();

        if(!createResult){
            Log.e(TAG, "Failed to create socket");
        }

        // connect GUI variables to values in layout
        mLoginButton = (Button) findViewById(R.id.login_button);
        mUsername = (EditText) findViewById(R.id.username_edit_text);
        mPassword = (EditText) findViewById(R.id.password_edit_text);
        mResultView = (TextView) findViewById(R.id.result_view);

        mResultView.setVisibility(View.INVISIBLE);

        // login button clicked function
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "LoginButton pressed");
                // get the entered username and password
                String username = mUsername.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                // ensure user input follows the correct format
                Boolean validate = validate(username, password);

                if (!validate){
                    mResultView.setText("Invalid Input. \nPlease ensure the username and password\n only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                if (!lockFlag) {
                    mResultView.setVisibility(View.INVISIBLE);

                    // create JSON object containing username to send to the server
                    JSONObject usernameRequest = new JSONObject();

                    usernameRequest.put("type", "get_user");
                    usernameRequest.put("payload", username);

                    Boolean sendResult = sendServer(usernameRequest);

                    // Login failed -> print error message
                    if(!sendResult){
                       return;
                    }

                    String receiveString = receivePacket();

                    // Login failed -> print error message
                    if (receiveString == null){
                       Log.e(TAG, "Failed to receive a string");
                       return;
                    }

                    LoginResult result = login(receiveString, password);

                    Log.i(TAG, "Completed Login Function");

                    // Change to correct activity if login is successful
                    if (result.getResult() && result.getDeveloper()) {
                        Log.i(TAG, "Successful login as developer");
                        sendLoad(username);
                        socket.close();
                        Intent DeveloperIntent = new Intent(LoginActivity.this, DeveloperActivity.class);
                        LoginActivity.this.startActivity(DeveloperIntent);
                    } else if (result.getResult()) {
                        Log.i(TAG, "Successful login as user");
                        sendLoad(username);
                        socket.close();
                        Intent SampleIntent = new Intent(LoginActivity.this, SampleActivity.class);
                        LoginActivity.this.startActivity(SampleIntent);
                    } else {
                        mResultView.setText(R.string.access_denied);
                        mResultView.setVisibility(View.VISIBLE);
                    }
                } else{
                    mResultView.setVisibility(View.VISIBLE);
                    mResultView.setText("Lockout due to maximum number of login attempts");
                }
            }
        });
    }

    /*
        Validates user input
        @param username The username of the user to be logged in
        @param password The password of the user to be logged in
        return Boolean Indicates if the user input is correctly formed
     */
    public Boolean validate(String username, String password){
        Boolean result;

        result = _processInput(username);

        if (!result){
            Log.w(TAG, "Invalid username");
            return false;
        }

        Log.d(TAG, "Username: " + username);

        result = _processInput(password);
        if (!result){
            Log.w(TAG, "Invalid password");
            return false;
        }

        Log.d(TAG, "Password: " + password);

        Log.d(TAG, "validate returning true");
        return true;
    }


    /*
        Determines if the login is correct
        @param json The JSON response from the server with user information in string format
        @param password The entered password
        return LoginResult Indicates if the login is correct and if the account belongs to a developer
    */
    public LoginResult login(String json, String password){
        JSONObject receiveJSON = null;

        // variables to store user information from database
        String databasePassword = "";
        byte[] salt = "".getBytes();
        String developerString;
        Boolean developer = false;

        Log.d(TAG, "json string: " + json);

        // pasrse json string into a JSON object to retrieve data
        try {
            receiveJSON = (JSONObject) new JSONParser().parse(json);
        } catch (ParseException e){
            Log.e(TAG, "Failed to parse JSON object");
            Log.e(TAG, "Exception: " + e);
            return new LoginResult(false, false);
        }

        // get type from JSON response
        String type = receiveJSON.get("type").toString();

        // check to see if response is an error or a correct response with type of user
        if(type.equalsIgnoreCase("error")){
            Log.e(TAG, "Received an error when response from server. Type: " + type);
            return new LoginResult(false, false);
        } else if (!type.equalsIgnoreCase("user")){
            Log.e(TAG, "Did no receive a user response from server. Type: " + type);
            return new LoginResult(false, false);
        }

        // get payloads of user JSON response
        JSONObject payloadObject = (JSONObject) receiveJSON.get("payload");

        // store the encrypted database password
        databasePassword = payloadObject.get("password").toString();

        Log.d(TAG, "database password: " + databasePassword);

        // store the salt value for encrypting the password
        salt = Base64.decode(payloadObject.get("salt").toString().getBytes(), Base64.DEFAULT);

        // store a value indicating if the user account belongs to a developer
        developerString = payloadObject.get("developer").toString();

        Log.d(TAG, "developer string: " + developerString);

        if (developerString.equals("1")){
            developer = true;
        } else {
            developer = false;
        }

        Log.d(TAG, "Password: " + password);

        // encrypts the entered password for comparison with database password
        String encryptedPassword = _hashPassword(password, salt);

        Log.d(TAG, "Encrypted password: " + encryptedPassword);

        if (encryptedPassword == null) {
            Log.w(TAG, "Error Hashing Password");
            return new LoginResult(false, false);
        } else if (databasePassword.equals(encryptedPassword)){
            numTries = 0;
            return new LoginResult(true, developer);
        }

        // increase number of login attempts
        numTries ++;

        // if number of login attempts equals 3, lock the login page for a se timeout period
        if (numTries == 3){
            numTries = 0;

            SleepTask sleepTask = new SleepTask();
            sleepTask.execute(SLEEPTIME);

            //mLoginButton.setEnabled(false);
            lockFlag = true;
        }

        // if you reach this point, the login attempt has failed
        return new LoginResult(false, false);
    }

    /*
        Helper process user input to ensure if follows the correct format
        @param input The string to be checked
        return Boolean Indicates if the user input follows the correct format
    */
    private Boolean _processInput(String input) {
        if (input == null){
            return false;
        } else if (!input.matches("[a-zA-Z0-9]+")){
            return false;
        }

        Log.d(TAG, "_processInput returning true");
        return true;
    }

    /*
        Helper function to hash the password using the PBKDF2WithHmacSHA1 algorithm
        @param password User entered password
        @param salt Salt value used ro encrypt password
        return String Encrypted password
    */
    private String _hashPassword(String password, byte[] salt){
        SecretKeyFactory factory;
        byte[] hash;

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e){
            Log.e(TAG, "No such algorithm exception when hashing password");
            e.printStackTrace();
            return null;
        }

        try {
            hash = factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "Invalid key exception when hashing password!");
            e.printStackTrace();
            return null;
        }

        Log.d(TAG, "hashed password: " + new String(Base64.encode(hash, Base64.DEFAULT)));
        return new String(Base64.encode(hash, Base64.DEFAULT));
    }

    /*
        Sends a load request to the server to load data for the ML Pi
        @param username Username for data to be loaded
        return Boolean Indicates if load request was successful
    */
    private Boolean sendLoad(String username){
        JSONObject receiveJSON;

        JSONObject sendJSON = new JSONObject();
        sendJSON.put("type", "load");
        sendJSON.put("payload", username);

        Boolean result = sendServer(sendJSON);

        if (!result){
            Log.e(TAG, "Failed to send load request");
            return false;
        }

        String response = receivePacket();

        if (response == null){
            Log.e(TAG, "Failed to receive response to load request");
            return false;
        }

        try {
            receiveJSON = (JSONObject) new JSONParser().parse(response);
        } catch (ParseException e){
            Log.e(TAG, "Failed to parse response");
            return false;
        }

        String type = receiveJSON.get("type").toString();

        if (!type.equalsIgnoreCase("ACK")){
            Log.e(TAG, "Did not receive an ACK response");
            return false;
        }

        return true;
    }

    /*
        Creates the Datagram socket used to send requests to the server
        return Boolean Indicates if the socket was created
    */
    protected Boolean createSocket(){
        try{
            socket = new DatagramSocket() ;
        } catch (SocketException e){
            Log.e(TAG, "Socket exception when creating socket and receiveSoc!");
            Log.e(TAG, "Socket Error:", e);
            e.printStackTrace();
            return false;
        }

        try{
            socket.setSoTimeout(30000);
        } catch (SocketException e){
            Log.e(TAG, "Socket exception when setting timeout!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
        Sends a JSON request to the server
        @param JSONObject JSON object to send to server
        return Boolean Indicates if request was sent successfully
    */
    protected Boolean sendServer(JSONObject sendJSON){
        byte[] send = sendJSON.toString().getBytes(UTF8_CHARSET);

        DatagramPacket packet = new DatagramPacket(send, send.length, ADDR, PORT);

        try {
            socket.send( packet );
        } catch (Exception e){
            Log.e(TAG, "Exception when sending packet!");
            Log.e(TAG, "Socket Exception:", e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /*
        Receives a response from the server
        return String Response from the server
    */
    protected String receivePacket(){
        DatagramPacket receivePacket = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE) ;
        try {
            socket.receive(receivePacket);
        } catch (Exception e){
            Log.e(TAG, "Exception when receiving packet!");
            Log.e(TAG, "Exception: ", e);
            e.printStackTrace();
            return null;
        }

        String receiveString = new String(receivePacket.getData()).trim();

        return receiveString;
    }

    /*
        Class that counts down the timeout for the locked login screen
        AsyncTask is used to prevent the app from hanging
    */
    private class SleepTask extends AsyncTask<Integer, Void, Void>{
        @Override
        protected Void doInBackground(Integer ...args){
            int sleepTime = args[0];
            try {
                TimeUnit.MINUTES.sleep(sleepTime);
            } catch (InterruptedException e){
                Log.e(TAG, "Interrupted Exception when sleeping for 5 minuets");
            }

            lockFlag = false;
            return null;
        }
    }
}
