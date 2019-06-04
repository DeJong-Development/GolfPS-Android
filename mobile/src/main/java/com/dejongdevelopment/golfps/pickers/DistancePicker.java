package com.dejongdevelopment.golfps.pickers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dejongdevelopment.golfps.R;

/**
 * Created by gdejong on 5/6/17.
 */

public class DistancePicker extends Activity {

    int clubNumber = 1;
    int avgDistance = 0;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_distancepicker);

        prefs = this.getSharedPreferences("com.flummoxedcosmos.golface", Context.MODE_PRIVATE);

        Intent i = getIntent();
        clubNumber = i.getIntExtra("clubnumber", 1);
        int distance = prefs.getInt("clubdistance" + clubNumber, 100);

        final EditText distanceText = (EditText) findViewById(R.id.dp_distance);
        final SeekBar seekBar = (SeekBar) findViewById(R.id.dp_seekBar);
        distanceText.setText(String.valueOf(distance));
        distanceText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                avgDistance = Integer.parseInt(distanceText.getText().toString());
                seekBar.setProgress(avgDistance - 50);
                return false;
            }
        });
        seekBar.setProgress(distance - 50);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                avgDistance = 50 + progress;
                distanceText.setText(String.valueOf(avgDistance));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Button okay = (Button) findViewById(R.id.dp_okayButton);
        okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set value to shared preferences...
                avgDistance = Integer.parseInt(distanceText.getText().toString());
                prefs.edit().putInt("clubdistance" + clubNumber, avgDistance).apply();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("club_number", clubNumber);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

    }
}
