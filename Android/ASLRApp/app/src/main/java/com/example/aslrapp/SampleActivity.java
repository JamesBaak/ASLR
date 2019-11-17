package com.example.aslrapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

enum Letter
{
    NONE, B, I, L, O, Y, ONE;
}

class UserActivity extends AppCompatActivity{

    private  final String TAG = this.getClass().getSimpleName() + " @" + System.identityHashCode(this);
    private ImageView mImage;
    private RadioGroup mGroup;
    private Button mSampleButton;
    private Boolean train = false;
    private Letter letter;
    private Letter result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImage = (ImageView) findViewById(R.id.aslLetter);
        mGroup = (RadioGroup) findViewById(R.id.button_group);
        mSampleButton = (Button) findViewById(R.id.sample_button);

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
                Log.i(TAG, "Sample Button pressed");

                if (letter == Letter.NONE){
                    // send sample request
                } else {

                }

                mSampleButton.setEnabled(false);

                // wait for response of timeout
                result = Letter.B;

                // display image
                switch(result) {
                    case B:
                        mImage.setImageResource(R.drawable.signB);
                        break;
                    case I:
                        mImage.setImageResource(R.drawable.signI);
                        break;
                    case L:
                        mImage.setImageResource(R.drawable.signL);
                        break;
                    case O:
                        mImage.setImageResource(R.drawable.signO);
                        break;
                    case Y:
                        mImage.setImageResource(R.drawable.signY);
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
}
