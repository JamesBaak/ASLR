package com.example.aslrapp;

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

public class SampleActivity extends AppCompatActivity{

    private  final String TAG = "SampleActivity";
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private ImageView mImage;
    private RadioGroup mGroup;
    private Button mSampleButton;
    private Letter letter;
    private Letter result;

    private int PORT = 9999;
    private InetAddress ADDR;
    private final static int PACKETSIZE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Boolean train = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

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

        mGroup.setVisibility(View.INVISIBLE);

        train = getIntent().getBooleanExtra("TRAIN_AI", false);

        letter = Letter.NONE;

        if (train){
            mGroup.setVisibility(View.VISIBLE);
        }

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

        mSampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject receiveJSON = null;
                Log.i(TAG, "Sample Button pressed");

                mSampleButton.setEnabled(false);

                Boolean sendResult = sendServer();

                if (!sendResult){
                    mSampleButton.setEnabled(true);
                }

                String receiveString = receivePacket();

                try {
                    receiveJSON = (JSONObject) new JSONParser().parse(receiveString);
                } catch (ParseException e){
                    Log.e(TAG, "Failed to parse return string");
                    return;
                }

                String type = receiveJSON.get("type").toString();

                if(!type.equalsIgnoreCase("prediction")){
                    Log.e(TAG, "Did not receive a reply with a prediction type");
                    return;
                }

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

    protected Boolean sendServer(){
        DatagramSocket socket = null;

        JSONObject sampleRequest = new JSONObject();

        sampleRequest.put("type", "sample");

        if (letter == Letter.NONE){
            sampleRequest.put("payload", "");
        } else {
            sampleRequest.put("payload", letter.getValue());
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
