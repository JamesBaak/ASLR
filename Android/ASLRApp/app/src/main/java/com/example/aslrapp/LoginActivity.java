package com.example.aslrapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

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

final class LoginResult {
    private final Boolean result;
    private final Boolean developer;

    public LoginResult(Boolean result, Boolean developer) {
        this.result = result;
        this.developer = developer;
    }

    public Boolean getResult() {
        return result;
    }

    public Boolean getDeveloper() {
        return developer;
    }
}

public class LoginActivity extends AppCompatActivity {

    private final String TAG = "LoginActivity";
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    protected Boolean lockFlag = false;
    private final int SLEEPTIME = 1;

    private Button mLoginButton;
    private EditText mUsername;
    private EditText mPassword;
    private TextView mResultView;

    private int PORT = 9999;
    private InetAddress ADDR;
    private final static int PACKETSIZE = 1000;

    private int numTries = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // TODO for testing purposes -> update when connected to Pi
        try {
            ADDR = InetAddress.getByName("10.0.2.2");
        } catch (UnknownHostException e){
            Log.e(TAG, "Unknown host exception when creating address!");
            e.printStackTrace();
        }

        mLoginButton = (Button) findViewById(R.id.login_button);
        mUsername = (EditText) findViewById(R.id.username_edit_text);
        mPassword = (EditText) findViewById(R.id.password_edit_text);
        mResultView = (TextView) findViewById(R.id.result_view);

        mResultView.setVisibility(View.INVISIBLE);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "LoginButton pressed");
                String username = mUsername.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                Boolean validate = validate(username, password);

                if (!validate){
                    mResultView.setText("Invalid Input. \nPlease ensure the username only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                if (!lockFlag) {
                    mResultView.setVisibility(View.INVISIBLE);

                    Boolean sendResult = sendServer(username);

                    // Login failed for some reason
                    if(!sendResult){
                       return;
                    }

                    JSONObject receiveJSON = receivePacket();

                    // Login failed for some reason
                    if (receiveJSON == null){
                       return;
                    }

                    //JSONObject receiveJSON = "{username: userTest, saltValue: [B@26dfc36, password: [B@f67b9d1, developer: 0}";

                    LoginResult result = login(receiveJSON, password);
                    Log.i(TAG, "Completed Login Function");

                    if (result.getResult() && result.getDeveloper()) {
                        Log.i(TAG, "Successful login as developer");
                        Intent DeveloperIntent = new Intent(LoginActivity.this, DeveloperActivity.class);
                        LoginActivity.this.startActivity(DeveloperIntent);
                    } else if (result.getResult()) {
                        Log.i(TAG, "Successful login as user");
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

    public LoginResult login (JSONObject receiveJSON, String password){
        String databasePassword = "";
        byte[] salt = "".getBytes();
        int developerInt = 0;
        Boolean developer = false;


        try {
            databasePassword = receiveJSON.get("password").toString();
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when getting databasePassword!");
            e.printStackTrace();
        }

        try {
            salt = receiveJSON.get("saltValue").toString().getBytes();
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when getting salt value!");
            e.printStackTrace();
        }

        try {
            developerInt = receiveJSON.getInt("developer");
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when getting developer status!");
            e.printStackTrace();
        }

        if (developerInt == 1){
            developer = true;
        }// else developer is false, which is the default

        String encryptedPassword = _hashPassword(password, salt);

        if (encryptedPassword == null) {
            Log.w(TAG, "Error Hashing Password");
            return new LoginResult(false, false);
        } else if (databasePassword.equals(encryptedPassword)){
            numTries = 0;
            return new LoginResult(true, developer);
        }

        numTries ++;

        if (numTries == 3){
            numTries = 0;

            SleepTask sleepTask = new SleepTask();
            sleepTask.execute(SLEEPTIME);

            //mLoginButton.setEnabled(false);
            lockFlag = true;
        }

        return new LoginResult(false, false);
    }

    private Boolean _processInput(String input) {
        if (input == null){
            return false;
        } else if (!input.matches("[a-zA-Z0-9]+")){
            return false;
        }

        Log.d(TAG, "_processInput returning true");
        return true;
    }

    private String _hashPassword(String password, byte[] salt){
        SecretKeyFactory factory;
        byte[] hash;

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        try {
            factory = SecretKeyFactory.getInstance("PBEwithHmacSHA1");
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
        return hash.toString();
    }

    protected Boolean sendServer(String username){
        DatagramSocket socket = null;

        // create JSON object containing username to send to the server
        JSONObject usernameRequest = new JSONObject();
        try {
            usernameRequest.put("type", "get_user");
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when adding type to usernameRequest!");
            e.printStackTrace();
        }

        try {
            usernameRequest.put("payload", username);
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when adding payload to usernameRequest!");
            e.printStackTrace();
        }

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

        byte[] sendJSON = usernameRequest.toString().getBytes(UTF8_CHARSET);

        DatagramPacket packet = new DatagramPacket(sendJSON, sendJSON.length, ADDR, PORT);

        try {
            socket.send( packet );
        } catch (Exception e){
            Log.e(TAG, "Exception when sending packet!");
            Log.e("Udp:", "Socket Exception:", e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    protected JSONObject receivePacket(){
        DatagramSocket receiveSoc = null;
        JSONObject receiveJSON = null;

        try{
            receiveSoc = new DatagramSocket(PORT) ;
        } catch (SocketException e){
            Log.e(TAG, "Socket exception when creating socket and receiveSoc!");
            Log.e(TAG, "Socket Error:", e);
            e.printStackTrace();
            return null;
        }

        try{
            receiveSoc.setSoTimeout(30000);
        } catch (SocketException e){
            Log.e(TAG, "Socket exception when setting timeout!");
            e.printStackTrace();
            return null;
        }

        DatagramPacket receivePacket = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE) ;
        try {
            receiveSoc.receive(receivePacket);
        } catch (Exception e){
            Log.e(TAG, "Exception when receiving packet!");
            Log.e(TAG, "Exception: ", e);
            e.printStackTrace();
            return null;
        }

        String receiveString = receivePacket.getData().toString();

        try {
            receiveJSON = new JSONObject(receiveString);
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when creating receiveJSON!");
            e.printStackTrace();
            return null;
        }

        Log.d(TAG, "receiveJSON: " + receiveJSON.toString());

        JSONObject payload = new JSONObject();

        /*try {
            payload = receiveJSON.getJSONObject("payload");
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when getting payload from receiveJSON!");
            e.printStackTrace();
        }*/

        return receiveJSON;
    }

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
