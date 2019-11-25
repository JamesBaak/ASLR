package com.example.aslrapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import android.util.Base64;

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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class CreateNewUserActivity extends AppCompatActivity{

    private final String TAG = "CreateNewUserActivity";
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private static final Random RANDOM = new SecureRandom();
    private EditText mUsername;
    private EditText mPassword;
    private EditText mConfirmPassword;
    private Button mCreateNewUserButton;
    private TextView mResultView;
    private Boolean developer = false;
    private CheckBox mDevBox;

    private int PORT = 9999;
    private InetAddress ADDR;
    private final static int PACKETSIZE = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_user);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mUsername = (EditText) findViewById(R.id.usernameEdit);
        mPassword = (EditText) findViewById(R.id.passwordEdit);
        mConfirmPassword = (EditText) findViewById(R.id.confirmPasswordEdit);
        mCreateNewUserButton = (Button) findViewById(R.id.create_new_user_button);
        mResultView = (TextView) findViewById(R.id.resultView);
        mDevBox = (CheckBox) findViewById(R.id.developerBox);

        // TODO for testing purposes -> update when connected to Pi
        try {
            ADDR = InetAddress.getByName("10.0.2.2");
        } catch (UnknownHostException e){
            Log.e(TAG, "Unknown host exception when creating address!");
            e.printStackTrace();
        }

        mResultView.setVisibility(View.INVISIBLE);

        developer = getIntent().getBooleanExtra("DEVELOPER", false);

        if(developer){
            mDevBox.setVisibility(View.VISIBLE);
        }


        mCreateNewUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "CreateNewUserButton pressed");
                String username = mUsername.getText().toString().trim();
                String password = mPassword.getText().toString().trim();
                String confirmPassword = mConfirmPassword.getText().toString().trim();
                Boolean dev = mDevBox.isChecked();

                Boolean validate = validate(username, password, confirmPassword);

                if (!validate){
                    mResultView.setText("Invalid Input. \nPlease ensure the username and password\n only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                createNewUser(username, password, confirmPassword, dev);
            }
        });
    }

    public Boolean validate(String username, String password, String confirmPassword){
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

        result = _processInput(confirmPassword);
        if (!result){
            Log.w(TAG, "Invalid confirmation password");
            return false;
        }

        Log.d(TAG, "Confirmation password: " + confirmPassword);

        Log.d(TAG, "validate returning true");
        return true;
    }

    public void createNewUser(String username, String password, String confirmPassword, Boolean developer){
        JSONObject receiveJSON = null;

        if (!confirmPassword.equals(password)){
            mResultView.setText(R.string.non_match_passwords_error);
            mResultView.setVisibility(View.VISIBLE);
            return;
        }

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        String encryptedPassword = _hashPassword(password, salt);

        for (int i = 0; i < salt.length; i++){
            Log.d(TAG, "salt:" + salt[i]);
        }

        String saltStr = new String(Base64.encode(salt, Base64.DEFAULT));

        Log.d(TAG, "Salt: " + saltStr);
        Log.d(TAG, "Encrypted password: " + encryptedPassword);

        //send user to database
        JSONObject usernameRequest = new JSONObject();

        usernameRequest.put("type", "create_user");

        Map m = new LinkedHashMap(4);
        m.put("username", username);
        m.put("saltValue", saltStr);
        m.put("password", encryptedPassword);
        m.put("developer", developer ? 1 : 0);

        usernameRequest.put("payload", m);

        Boolean sendResult = sendServer(usernameRequest);

        // Create new user request failed for some reason
        if(!sendResult){
            return;
        }

        String receiveString = receivePacket();

        // Create new user request failed for some reason
        if (receiveString == null){
            return;
        }

        try {
            receiveJSON = (JSONObject) new JSONParser().parse(receiveString);
        } catch (ParseException e){
        }

        String ack = "";
        ack = receiveJSON.get("type").toString();

        Boolean result;

        if (ack.equalsIgnoreCase("ACK")){
            result = true;
        } else {
            result = false;
        }

        if (result){
            mResultView.setText(R.string.success_new_user);
            mResultView.setVisibility(View.VISIBLE);

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e){
                Log.e(TAG, "Sleep Interrupted");
            }

            Intent MainIntent = new Intent(CreateNewUserActivity.this, MainActivity.class);
            CreateNewUserActivity.this.startActivity(MainIntent);
        } else {
            mResultView.setVisibility(View.VISIBLE);
            mResultView.setText(R.string.fail_create_user);
        }
    }

    private Boolean _processInput(String input) {
        if (input == null){
            return false;
        } else if (!input.matches("[a-zA-Z0-9]+")){
            return false;
        }

        return true;
    }

    private String _hashPassword(String password, byte[] salt){
        SecretKeyFactory factory;
        byte[] hash;

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e){
            Log.i(TAG, "No such algorithm: PBKDF2WithHmacSHA1");
            e.printStackTrace();
            return null;
        }

        try {
            hash = factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            Log.i(TAG, "Invalid key exception!");
            e.printStackTrace();
            return null;
        }
        return new String(Base64.encode(hash, Base64.DEFAULT));
    }

    protected Boolean sendServer(JSONObject jsonPacket){
        DatagramSocket socket = null;

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

        byte[] sendJSON = jsonPacket.toString().getBytes(UTF8_CHARSET);

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

    protected String receivePacket(){
        DatagramSocket receiveSoc = null;

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

        return receivePacket.getData().toString();

    }
}
