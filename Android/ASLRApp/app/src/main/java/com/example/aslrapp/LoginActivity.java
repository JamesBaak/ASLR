package com.example.aslrapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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


class InputException extends Exception {
    InputException(String str) { super(str); }
}

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

public class LoginActivity extends AppCompatActivity{

    private  final String TAG = "LoginActivity";

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

        mLoginButton = (Button) findViewById(R.id.login_button);
        mUsername = (EditText) findViewById(R.id.username_edit_text);
        mPassword = (EditText) findViewById(R.id.password_edit_text);
        mResultView = (TextView) findViewById(R.id.result_view);

        mResultView.setVisibility(View.INVISIBLE);

        Log.i(TAG, "Before Login");

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "LoginButton pressed");
                String username = mUsername.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                try {
                    _processInput(username);
                }catch (InputException e){
                    Log.w(TAG, "Invalid username");
                    mResultView.setText("Invalid Username. \nPlease ensure the username only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                try {
                    _processInput(password);
                }catch (InputException e){
                    Log.w(TAG, "Invalid password");
                    mResultView.setText("Invalid Password. \nPlease ensure the password only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                LoginResult result = login(username, password);
                Log.i(TAG, "Completed Login Function");

                if (result.getResult() && result.getDeveloper()){
                    Log.i(TAG, "Successful login as developer");
                    Intent DeveloperIntent = new Intent(LoginActivity.this, DeveloperActivity.class);
                    LoginActivity.this.startActivity(DeveloperIntent);
                } else if (result.getResult()){
                    Log.i(TAG, "Successful login as user");
                    Intent SampleIntent = new Intent(LoginActivity.this, SampleActivity.class);
                    LoginActivity.this.startActivity(SampleIntent);
                } // else login not successful and user stays on login page
            }
        });

    }

    public LoginResult login (String username, String password){
        final Charset UTF8_CHARSET = Charset.forName("UTF-8");
        DatagramSocket socket = null;
        DatagramSocket receiveSoc = null;

        String databasePassword = "";
        byte[] salt = "".getBytes();
        int developerInt = 0;
        Boolean developer = false;

        // TODO for testing purposes -> update when connected to Pi
        try {
            ADDR = InetAddress.getByName("10.0.2.2");
        } catch (UnknownHostException e){
            Log.e(TAG, "Unknown host exception when creating address!");
            e.printStackTrace();
            return new LoginResult(false, false);
        }

        // get salt value, databasePassword and developer status
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
            receiveSoc = new DatagramSocket(PORT) ;
        } catch (SocketException e){
            Log.e(TAG, "Socket exception when creating socket and receiveSoc!");
            Log.e("Udp:", "Socket Error:", e);
            e.printStackTrace();
            return new LoginResult(false, false);
        }


        try{
            socket.setSoTimeout(30000);
        } catch (SocketException e){
            Log.e(TAG, "Socket exception when setting timeout!");
            e.printStackTrace();
            return new LoginResult(false, false);
        }

        byte[] sendJSON = usernameRequest.toString().getBytes(UTF8_CHARSET);

        DatagramPacket packet = new DatagramPacket(sendJSON, sendJSON.length, ADDR, PORT) ;

        try {
            socket.send( packet );
        } catch (IOException e){
            Log.e(TAG, "IO exception when sending packet!");
            e.printStackTrace();
            return new LoginResult(false, false);
        }

        DatagramPacket receivePacket = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE) ;
        try {
            receiveSoc.receive(receivePacket);
        } catch (IOException e){
            Log.e(TAG, "IO exception when receiving packet!");
            e.printStackTrace();
            return new LoginResult(false, false);
        }

        JSONObject receiveJSON;

        try {
            receiveJSON = new JSONObject(receivePacket.getData().toString());
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when creating receiveJSON!");
            e.printStackTrace();
            return new LoginResult(false, false);
        }

        JSONObject payload = new JSONObject();

        try {
            payload = receiveJSON.getJSONObject("payload");
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when getting payload from receiveJSON!");
            e.printStackTrace();
        }

        try {
            databasePassword = payload.get("password").toString();
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when getting databasePassword!");
            e.printStackTrace();
        }

        try {
            salt = payload.get("salt").toString().getBytes();
        } catch (JSONException e){
            Log.e(TAG, "JSON exception when getting salt value!");
            e.printStackTrace();
        }

        try {
            developerInt = payload.getInt("developer");
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
            mResultView.setText(R.string.hash_error);
            mResultView.setVisibility(View.VISIBLE);
        } else if (databasePassword.equals(encryptedPassword)){
            mResultView.setVisibility(View.INVISIBLE);
            numTries = 0;
            return new LoginResult(true, developer);
        }

        numTries ++;
        mResultView.setText(R.string.access_denied);
        mResultView.setVisibility(View.VISIBLE);

        if (numTries == 3){
            numTries = 0;

            mResultView.setText("Five minute timeout:\n too many login attempts");
            mResultView.setVisibility(View.VISIBLE);

            mLoginButton.setEnabled(false);
            try {
                TimeUnit.MINUTES.sleep(5);
            } catch (InterruptedException e){
                Log.e(TAG, "Interrupted Exception when sleeping for 5 minuets");
            }

            mLoginButton.setEnabled(true);
        }

        return new LoginResult(false, false);
    }

    private void _processInput(String input) throws InputException{
        if (input == null){
            throw new InputException("Empty input");
        } else if (!input.matches("[a-zA-Z0-9]+")){
            throw new InputException("Non alphanumeric characters in input");
        }
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



}
