package com.dejongdevelopment.golfps.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.dejongdevelopment.golfps.activities.StartActivity;

/**
 * Created by gdejong on 5/8/17.
 */

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
    }
}
