package com.example.aslrapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private  final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Button mCreateNewUserButton;
        Button mLoginButton;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCreateNewUserButton = (Button) findViewById(R.id.create_user_button);
        mLoginButton = (Button) findViewById(R.id.login_button);

        mCreateNewUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "CreateNewUserButton pressed");
                Intent CreateUserIntent = new Intent(MainActivity.this, CreateNewUserActivity.class);
                MainActivity.this.startActivity(CreateUserIntent);
            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "LoginButton pressed");
                Intent LoginIntent = new Intent(MainActivity.this, LoginActivity.class);
                MainActivity.this.startActivity(LoginIntent);
            }
        });
    }
}
