package com.example.aslrapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;

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


// ASL letter enum
enum Letter
{
    B(1), I(2), L(3), O(4), Y(5), ONE(6), NONE (0);

    // integer value of enum for sending to server
    private final int value;

    Letter(int value) {
        /*
         * Enum Constructor
         * param: value integer value of the enum
         */
        this.value = value;
    }

    public int getValue() {
        /*
         * Getter function for enum value
         * return int: integer value of enum
         */
        return value;
    }
}

/*
    The class for the sample activity
    Controls the functionality to send a sample request
 */
public class SampleActivity extends AppCompatActivity{

    // a tag for logging
    private  final String TAG = "SampleActivity";
    // charset for encoding and decoding byte arrays to Strings
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    // displays the value of the sample result
    private ImageView mImage;
    // buttons to select the type of request sent for training mode
    private RadioGroup mGroup;
    // sample request button
    private Button mSampleButton;
    // letter for training data
    private Letter letter;
    // letter received from ML Pi
    private Letter result;

    // Port and address information for sending requests to the server
    private int PORT = 9999;
    private InetAddress ADDR;
    private final static int PACKETSIZE = 1024;

    DatagramSocket socket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Boolean train = false;

        // activity setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // turns off the default policy of having no network operations in the main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mImage = (ImageView) findViewById(R.id.aslLetter);
        mGroup = (RadioGroup) findViewById(R.id.button_group);
        mSampleButton = (Button) findViewById(R.id.sample_button);

        // TODO for testing purposes -> update when connected to Pi
        try {
            ADDR = InetAddress.getByName("10.0.2.2");
        } catch (UnknownHostException e){
            Log.e(TAG, "Unknown host exception when creating address!");
            e.printStackTrace();
        }

        // create socket to send data
        Boolean createResult = createSocket();

        if (!createResult){
            Log.e(TAG, "Failed to create socket");
        }

        mGroup.setVisibility(View.INVISIBLE);

        // get value from developer activity indicting if the ML algorithm is being trained
        train = getIntent().getBooleanExtra("TRAIN_AI", false);

        letter = Letter.NONE;

        if (train){
            mGroup.setVisibility(View.VISIBLE);
        }

        // select the letter to be sent when training the ML algorithm
        mGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.B_button:
                        letter = Letter.B;
                        break;
                    case R.id.I_button:
                        letter = Letter.I;
                        break;
                    case R.id.L_button:
                        letter = Letter.L;
                        break;
                    case R.id.O_button:
                        letter = Letter.O;
                        break;
                    case R.id.Y_button:
                        letter = Letter.Y;
                        break;
                    case R.id.one_button:
                        letter = Letter.ONE;
                        break;
                    default:
                        letter = Letter.NONE;

                }
            }
        });

        // sample button clicked function
        mSampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject receiveJSON = null;
                Log.i(TAG, "Sample Button pressed");

                // disable sample button
                mSampleButton.setEnabled(false);

                // send sample request to server
                Boolean sendResult = sendServer();

                if (!sendResult){
                    mSampleButton.setEnabled(true);
                }

                // receive response from server
                // the first response is an acknowledgment that the request was received
                String receiveString = receivePacket();

                Log.d(TAG, "receive string: " + receiveString);

                try {
                    receiveJSON = (JSONObject) new JSONParser().parse(receiveString);
                } catch (ParseException e){
                    Log.e(TAG, "Failed to parse return string");
                    return;
                }

                // ensure response form server is correct
                String type = receiveJSON.get("type").toString();

                if(!type.equalsIgnoreCase("ack")){
                    Log.e(TAG, "Didn't receive acknowledge of sample request");
                    mSampleButton.setEnabled(true);
                    return;
                }

                // receive response from server
                // the second response contains the response from the ML Pi
                receiveString = receivePacket();

                Log.d(TAG, "receive string: " + receiveString);

                try {
                    receiveJSON = (JSONObject) new JSONParser().parse(receiveString);
                } catch (ParseException e){
                    Log.e(TAG, "Failed to parse return string");
                    return;
                }

                type = receiveJSON.get("type").toString();

                if(type.equalsIgnoreCase("error")){
                    String errorStr = receiveJSON.get("payload").toString();
                    Log.e(TAG, "Error: " + errorStr);
                    mSampleButton.setEnabled(true);
                    return;
                } else if(!type.equalsIgnoreCase("prediction")){
                    Log.e(TAG, "Did not receive a reply with a prediction type");
                    mSampleButton.setEnabled(true);
                    return;
                }

                // get the predicted letter from the response
                int letterInt = ((int) receiveJSON.get("payload"));

                result = fromInteger(letterInt);

                // display the correct ASL letter image
                switch(result) {
                    case B:
                        mImage.setImageResource(R.drawable.signb);
                        break;
                    case I:
                        mImage.setImageResource(R.drawable.signi);
                        break;
                    case L:
                        mImage.setImageResource(R.drawable.signl);
                        break;
                    case O:
                        mImage.setImageResource(R.drawable.signo);
                        break;
                    case Y:
                        mImage.setImageResource(R.drawable.signy);
                        break;
                    case ONE:
                        mImage.setImageResource(R.drawable.sign1);
                        break;
                    default:
                        mImage.setImageResource(R.drawable.clock);
                }

                mSampleButton.setEnabled(true);
            }
        });
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
        return Boolean Indicates if request was sent successfully
    */
    protected Boolean sendServer(){
        JSONObject sampleRequest = new JSONObject();

        sampleRequest.put("type", "sample");

        if (letter == Letter.NONE){
            sampleRequest.put("payload", "");
        } else {
            sampleRequest.put("payload", letter.getValue());
        }

        byte[] sendJSON = sampleRequest.toString().getBytes(UTF8_CHARSET);

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

        return new String(receivePacket.getData()).trim();
    }

    /*
        Converts an integer into the correct letter enum
     */
    private static Letter fromInteger(int x) {
        switch(x) {
            case 1:
                return Letter.B;
            case 2:
                return Letter.I;
            case 3:
                return Letter.L;
            case 4:
                return Letter.O;
            case 5:
                return Letter.Y;
            case 6:
                return Letter.ONE;
            default:
                return Letter.NONE;
        }
    }
}
