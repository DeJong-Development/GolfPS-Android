package com.dejongdevelopment.golfps.pickers;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.dejongdevelopment.golfps.R;

/**
 * Created by gdejong on 5/6/17.
 */

public class HoleNumberPicker extends Activity {

    int holeNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_holepicker);

        Intent i = getIntent();
        int startNum = i.getIntExtra("current_hole_number", 1);
        int numHoles = i.getIntExtra("maxnumberofholes", 18);

        NumberPicker np = (NumberPicker) findViewById(R.id.numberPicker);

        //Populate NumberPicker values from minimum and maximum value range
        //Set the minimum value of NumberPicker
        np.setMinValue(1);
        //Specify the maximum value/number of NumberPicker
        np.setMaxValue(numHoles);

        np.setValue(startNum);

        //Gets whether the selector wheel wraps when reaching the min/max value.
        np.setWrapSelectorWheel(true);

        //Set a value change listener for NumberPicker
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                //Display the newly selected number from picker
                Log.d("number selected", "#" + newVal);
                holeNumber = newVal;
            }
        });

        Button okay = (Button) findViewById(R.id.hp_okayButton);
        okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("hole_number", holeNumber);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

    }
}
