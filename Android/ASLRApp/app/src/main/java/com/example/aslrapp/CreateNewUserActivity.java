package com.example.aslrapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

    private  final String TAG = this.getClass().getSimpleName() + " @" + System.identityHashCode(this);
    private static final Random RANDOM = new SecureRandom();
    private EditText mUsername;
    private EditText mPassword;
    private EditText mConfirmPassword;
    private Button mCreateNewUserButton;
    private TextView mResultView;
    private Boolean developer = false;
    private CheckBox mDevBox;

    private int PORT = 9999;
    // TODO for testing puproses -> update when connected to Pi
    private InetAddress ADDR;
    private final static int PACKETSIZE = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_user);

        mUsername = (EditText) findViewById(R.id.usernameEdit);
        mPassword = (EditText) findViewById(R.id.passwordEdit);
        mConfirmPassword = (EditText) findViewById(R.id.confirmPasswordEdit);
        mCreateNewUserButton = (Button) findViewById(R.id.create_new_user_button);
        mResultView = (TextView) findViewById(R.id.resultView);
        mDevBox = (CheckBox) findViewById(R.id.developerBox);

        mResultView.setVisibility(View.INVISIBLE);

        developer = getIntent().getBooleanExtra("DEVELOPER", false);

        if(developer){
            mDevBox.setVisibility(View.VISIBLE);
        }


        mCreateNewUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "LoginButton pressed");
                String username = mUsername.getText().toString().trim();
                String password = mPassword.getText().toString().trim();
                String confirmPassword = mConfirmPassword.getText().toString().trim();
                Boolean dev = mDevBox.isChecked();

                try {
                    _processInput(username);
                } catch (InputException e) {
                    mResultView.setText("Invalid Username\n. Please ensure the username only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                try {
                    _processInput(password);
                } catch (InputException e) {
                    mResultView.setText("Invalid Password\n. Please ensure the password only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                try {
                    _processInput(confirmPassword);
                } catch (InputException e) {
                    mResultView.setText("Invalid Confirmation Password\n. Please ensure the password only contains alphanumeric characters");
                    mResultView.setVisibility(View.VISIBLE);
                    return;
                }

                createNewUser(username, password, confirmPassword, dev);
            }
        });
    }

    public void createNewUser(String username, String password, String confirmPassword, Boolean developer){
        final Charset UTF8_CHARSET = Charset.forName("UTF-8");
        DatagramSocket socket = null;
        DatagramSocket receiveSoc = null;

        if (!confirmPassword.equals(password)){
            mResultView.setText(R.string.non_match_passwords_error);
            mResultView.setVisibility(View.VISIBLE);
            return;
        }

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        String encryptedPassword = _hashPassword(password, salt);

        //send user to database
        JSONObject usernameRequest = new JSONObject();
        try {
            usernameRequest.put("type", "create_user");
        } catch (JSONException e){
            Log.i(TAG, "JSON exception!");
            e.printStackTrace();
        }

        Map m = new LinkedHashMap(4);
        m.put("username", username);
        m.put("saltValue", salt.toString());
        m.put("password", encryptedPassword);
        m.put("developer", developer ? 1 : 0);

        try {
            usernameRequest.put("payload", m);
        } catch (JSONException e){
            Log.i(TAG, "JSON exception!");
            e.printStackTrace();
        }

        try{
            socket = new DatagramSocket() ;
            receiveSoc = new DatagramSocket(PORT) ;
        } catch (SocketException e){
            Log.i(TAG, "Socket exception!");
            e.printStackTrace();
            return;
        }

        socket.connect(ADDR, PORT);

        try{
            socket.setSoTimeout(30000);
        } catch (SocketException e){
            Log.i(TAG, "Socket exception!");
            e.printStackTrace();
            return;
        }

        byte[] sendJSON = usernameRequest.toString().getBytes(UTF8_CHARSET);

        DatagramPacket packet = new DatagramPacket(sendJSON, sendJSON.length) ;

        try {
            socket.send( packet );
        } catch (IOException e){
            Log.i(TAG, "IO exception!");
            e.printStackTrace();
            return;
        }

        DatagramPacket receivePacket = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE) ;
        try {
            receiveSoc.receive(receivePacket);
        } catch (IOException e){
            Log.i(TAG, "IO exception!");
            e.printStackTrace();
            return;
        }

        JSONObject receiveJSON;

        try {
            receiveJSON = new JSONObject(receivePacket.getData().toString());
        } catch (JSONException e){
            Log.i(TAG, "JSON exception!");
            e.printStackTrace();
            return;
        }

        String awk = "";
        try {
            awk = receiveJSON.getString("type");
        } catch (JSONException e){
            Log.i(TAG, "JSON exception!");
            e.printStackTrace();
        }

        Boolean result;

        if (awk.equalsIgnoreCase("AWK")){
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
                Log.i(TAG, "Sleep Interrupted");
            }

            Intent MainIntent = new Intent(CreateNewUserActivity.this, MainActivity.class);
            CreateNewUserActivity.this.startActivity(MainIntent);
        } else {
            // TODO print error message if user was not created successfully
        }
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
            Log.i(TAG, "No such algorithm: PBEwithHmacSHA1");
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
        return hash.toString();
    }
}
