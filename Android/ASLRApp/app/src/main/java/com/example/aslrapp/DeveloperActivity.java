package com.example.aslrapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/*
    The class for the developer activity
    Contains all the developer options
 */
public class DeveloperActivity  extends AppCompatActivity {

    // a tag for logging
    private  final String TAG = this.getClass().getSimpleName() + " @" + System.identityHashCode(this);

    // buttons for selecting developer options
    private Button mCreateNewUserButton;
    private Button mSampleButton;
    private Button mTrainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // activity setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // connect GUI variables to values in layout
        mCreateNewUserButton = (Button) findViewById(R.id.create_new_user_button);
        mSampleButton = (Button) findViewById(R.id.sign_letter_button);
        mTrainButton = (Button) findViewById(R.id.train_button);

        // create a new user button clicked function
        mCreateNewUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "CreateNewUserButton pressed");
                Intent CreateUserIntent = new Intent(DeveloperActivity.this, CreateNewUserActivity.class);
                CreateUserIntent.putExtra("DEVELOPER", true);
                DeveloperActivity.this.startActivity(CreateUserIntent);
            }
        });

        // sample button clicked function
        mSampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "SampleButton pressed");
                Intent SampleIntent = new Intent(DeveloperActivity.this, SampleActivity.class);
                DeveloperActivity.this.startActivity(SampleIntent);
            }
        });

        // developer button clicked function
        mTrainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "TrainButton pressed");
                Intent TrainIntent = new Intent(DeveloperActivity.this, SampleActivity.class);
                TrainIntent.putExtra("TRAIN_AI", true);
                DeveloperActivity.this.startActivity(TrainIntent);
            }
        });

    }
}
