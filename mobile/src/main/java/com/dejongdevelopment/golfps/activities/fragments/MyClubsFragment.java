package com.dejongdevelopment.golfps.activities.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dejongdevelopment.golfps.R;
import com.dejongdevelopment.golfps.activities.MapsActivity;
import com.dejongdevelopment.golfps.pickers.DistancePicker;

/**
 * Created by gdejong on 6/21/17.
 */

public class MyClubsFragment extends Fragment {

    private Context context;
    private SharedPreferences prefs;

    Button club1Button;
    Button club2Button;
    Button club3Button;
    Button club4Button;
    Button club5Button;
    Button club6Button;
    Button club7Button;
    Button club8Button;
    Button club9Button;
    Button club10Button;
    Button club11Button;
    Button club12Button;
    Button club13Button;

    EditText club1EditText;
    EditText club2EditText;
    EditText club3EditText;
    EditText club4EditText;
    EditText club5EditText;
    EditText club6EditText;
    EditText club7EditText;
    EditText club8EditText;
    EditText club9EditText;
    EditText club10EditText;
    EditText club11EditText;
    EditText club12EditText;
    EditText club13EditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_myclubs, container, false);
        context = getContext();

        prefs = ((MapsActivity) getActivity()).getPrefs();

        club1Button = (Button) rootView.findViewById(R.id.club1Distance);
        club2Button = (Button) rootView.findViewById(R.id.club2Distance);
        club3Button = (Button) rootView.findViewById(R.id.club3Distance);
        club4Button = (Button) rootView.findViewById(R.id.club4Distance);
        club5Button = (Button) rootView.findViewById(R.id.club5Distance);
        club6Button = (Button) rootView.findViewById(R.id.club6Distance);
        club7Button = (Button) rootView.findViewById(R.id.club7Distance);
        club8Button = (Button) rootView.findViewById(R.id.club8Distance);
        club9Button = (Button) rootView.findViewById(R.id.club9Distance);
        club10Button = (Button) rootView.findViewById(R.id.club10Distance);
        club11Button = (Button) rootView.findViewById(R.id.club11Distance);
        club12Button = (Button) rootView.findViewById(R.id.club12Distance);
        club13Button = (Button) rootView.findViewById(R.id.club13Distance);
        club1EditText = (EditText) rootView.findViewById(R.id.club1);
        club2EditText = (EditText) rootView.findViewById(R.id.club2);
        club3EditText = (EditText) rootView.findViewById(R.id.club3);
        club4EditText = (EditText) rootView.findViewById(R.id.club4);
        club5EditText = (EditText) rootView.findViewById(R.id.club5);
        club6EditText = (EditText) rootView.findViewById(R.id.club6);
        club7EditText = (EditText) rootView.findViewById(R.id.club7);
        club8EditText = (EditText) rootView.findViewById(R.id.club8);
        club9EditText = (EditText) rootView.findViewById(R.id.club9);
        club10EditText = (EditText) rootView.findViewById(R.id.club10);
        club11EditText = (EditText) rootView.findViewById(R.id.club11);
        club12EditText = (EditText) rootView.findViewById(R.id.club12);
        club13EditText = (EditText) rootView.findViewById(R.id.club13);

        setListenersAndDefaultValues();

        return rootView;
    }

    private void setListenersAndDefaultValues() {

        club1Button.setOnClickListener(distancePicker(1));
        club2Button.setOnClickListener(distancePicker(2));
        club3Button.setOnClickListener(distancePicker(3));
        club4Button.setOnClickListener(distancePicker(4));
        club5Button.setOnClickListener(distancePicker(5));
        club6Button.setOnClickListener(distancePicker(6));
        club7Button.setOnClickListener(distancePicker(7));
        club8Button.setOnClickListener(distancePicker(8));
        club9Button.setOnClickListener(distancePicker(9));
        club10Button.setOnClickListener(distancePicker(10));
        club11Button.setOnClickListener(distancePicker(11));
        club12Button.setOnClickListener(distancePicker(12));
        club13Button.setOnClickListener(distancePicker(13));
        club1Button.setText(prefs.getInt("clubdistance1", 250) + " yds");
        club2Button.setText(prefs.getInt("clubdistance2", 225) + " yds");
        club3Button.setText(prefs.getInt("clubdistance3", 210) + " yds");
        club4Button.setText(prefs.getInt("clubdistance4", 200) + " yds");
        club5Button.setText(prefs.getInt("clubdistance5", 190) + " yds");
        club6Button.setText(prefs.getInt("clubdistance6", 180) + " yds");
        club7Button.setText(prefs.getInt("clubdistance7", 170) + " yds");
        club8Button.setText(prefs.getInt("clubdistance8", 160) + " yds");
        club9Button.setText(prefs.getInt("clubdistance9", 150) + " yds");
        club10Button.setText(prefs.getInt("clubdistance10", 140) + " yds");
        club11Button.setText(prefs.getInt("clubdistance11", 130) + " yds");
        club12Button.setText(prefs.getInt("clubdistance12", 100) + " yds");
        club13Button.setText(prefs.getInt("clubdistance13", 80) + " yds");

        club1EditText.setOnEditorActionListener(clubNamePicker(1));
        club2EditText.setOnEditorActionListener(clubNamePicker(2));
        club3EditText.setOnEditorActionListener(clubNamePicker(3));
        club4EditText.setOnEditorActionListener(clubNamePicker(4));
        club5EditText.setOnEditorActionListener(clubNamePicker(5));
        club6EditText.setOnEditorActionListener(clubNamePicker(6));
        club7EditText.setOnEditorActionListener(clubNamePicker(7));
        club8EditText.setOnEditorActionListener(clubNamePicker(8));
        club9EditText.setOnEditorActionListener(clubNamePicker(9));
        club10EditText.setOnEditorActionListener(clubNamePicker(10));
        club11EditText.setOnEditorActionListener(clubNamePicker(11));
        club12EditText.setOnEditorActionListener(clubNamePicker(12));
        club13EditText.setOnEditorActionListener(clubNamePicker(13));
        club1EditText.setText(prefs.getString("clubname1", "Driver"));
        club2EditText.setText(prefs.getString("clubname2", "5 Wood"));
        club3EditText.setText(prefs.getString("clubname3", "3 Wood"));
        club4EditText.setText(prefs.getString("clubname4", "3 Iron"));
        club5EditText.setText(prefs.getString("clubname5", "4 Iron"));
        club6EditText.setText(prefs.getString("clubname6", "5 Iron"));
        club7EditText.setText(prefs.getString("clubname7", "6 Iron"));
        club8EditText.setText(prefs.getString("clubname8", "7 Iron"));
        club9EditText.setText(prefs.getString("clubname9", "8 Iron"));
        club10EditText.setText(prefs.getString("clubname10", "9 Iron"));
        club11EditText.setText(prefs.getString("clubname11", "Pitching Wedge"));
        club12EditText.setText(prefs.getString("clubname12", "Sand Wedge"));
        club13EditText.setText(prefs.getString("clubname13", "Gap Wedge"));
    }

    private TextView.OnEditorActionListener clubNamePicker(final int clubNumber) {
        return new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                prefs.edit().putString("clubname" + clubNumber, v.getText().toString()).apply();
                return false;
            }
        };
    }

    private View.OnClickListener distancePicker(final int clubNumber) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, DistancePicker.class);
                intent.putExtra("clubnumber", clubNumber);
                startActivityForResult(intent, 9174); //distance picker will go to main activity
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 9174) {
            //just got some information back from the distance picker
            if (resultCode == getActivity().RESULT_OK) {

                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                int clubNumber = data.getIntExtra("club_number", 1);
                int distance = prefs.getInt("clubdistance" + clubNumber, 100);
                String dYd = distance + " yds";
                switch (clubNumber) {
                    case 1: club1Button.setText(dYd); break;
                    case 2: club2Button.setText(dYd); break;
                    case 3: club3Button.setText(dYd); break;
                    case 4: club4Button.setText(dYd); break;
                    case 5: club5Button.setText(dYd); break;
                    case 6: club6Button.setText(dYd); break;
                    case 7: club7Button.setText(dYd); break;
                    case 8: club8Button.setText(dYd); break;
                    case 9: club9Button.setText(dYd); break;
                    case 10: club10Button.setText(dYd); break;
                    case 11: club11Button.setText(dYd); break;
                    case 12: club12Button.setText(dYd); break;
                    case 13: club13Button.setText(dYd); break;
                }
            }

        }
    }
}
