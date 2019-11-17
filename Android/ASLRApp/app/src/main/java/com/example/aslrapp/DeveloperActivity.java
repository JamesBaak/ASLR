package com.example.aslrapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DeveloperActivity  extends AppCompatActivity {

    private  final String TAG = this.getClass().getSimpleName() + " @" + System.identityHashCode(this);
    private Button mCreateNewUserButton;
    private Button mSampleButton;
    private Button mTrainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCreateNewUserButton = (Button) findViewById(R.id.create_new_user_button);
        mSampleButton = (Button) findViewById(R.id.sign_letter_button);
        mTrainButton = (Button) findViewById(R.id.train_button);

        mCreateNewUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "CreateNewUserButton pressed");
                Intent CreateUserIntent = new Intent(DeveloperActivity.this, CreateNewUserActivity.class);
                DeveloperActivity.this.startActivity(CreateUserIntent);
            }
        });

        mSampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "SampleButton pressed");
                Intent SampleIntent = new Intent(DeveloperActivity.this, SampleActivity.class);
                DeveloperActivity.this.startActivity(SampleIntent);
            }
        });

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
