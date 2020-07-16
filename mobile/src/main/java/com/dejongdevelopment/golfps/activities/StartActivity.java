package com.dejongdevelopment.golfps.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dejongdevelopment.golfps.models.Course;
import com.dejongdevelopment.golfps.pickers.HoleNumberPicker;
import com.dejongdevelopment.golfps.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by gdejong on 5/22/17.
 */

public class StartActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_RESOLVE_ERROR = 13;
    private static final String DIALOG_ERROR = "dialog_error";

    private Context context = this;
    private TextView currentHoleText;
    private EditText searchBox;

    private ArrayAdapter<String> locationsArrayAdapter;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private int currentHoleNumber = 1;
    private HashMap<String, String> courseIdDictionary = new HashMap<String, String>();

    private ArrayList<Course> courseList = new ArrayList<>();

    private FirebaseFirestore db;
    private Vibrator vibe;

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_start);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        db = FirebaseFirestore.getInstance();
        queryCourses("");

        // Will need to use this when using current location to determine closest courses
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        
        currentHoleText = findViewById(R.id.start_holeNumber);
        searchBox = findViewById(R.id.start_locationFilter);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                String courseSearchText = s.toString();
                queryCourses(courseSearchText);
            }
        });

        Button holeNumberButton = findViewById(R.id.start_selectHoleNumber);
        holeNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, HoleNumberPicker.class);
                startActivityForResult(intent, 475);
            }
        });

        locationsArrayAdapter = new ArrayAdapter<>(this, R.layout.location_list_name);
        ListView locationList = findViewById(R.id.start_list);
        locationList.setAdapter(locationsArrayAdapter);
        locationList.setOnItemClickListener(mDeviceClickListener);
    }

    private void queryCourses(String name) {

        db.collection("courses")
                .whereGreaterThanOrEqualTo("name", name)
                .whereLessThan("name", name + "z")
                .orderBy("name")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            courseIdDictionary.clear();
                            locationsArrayAdapter.clear();

                            courseList.clear();
                            Course golfCourse = null;

                            for (DocumentSnapshot document : task.getResult()) {
                                String courseCity = document.getString("city");
                                String courseState = document.getString("state");
                                String realCourseName = document.getString("name");
                                String itemId = document.getId();

                                courseIdDictionary.put(realCourseName, itemId);
                                locationsArrayAdapter.add(realCourseName);

                                golfCourse = new Course(itemId);
                                golfCourse.name = realCourseName;
                                golfCourse.id = itemId;
                                golfCourse.city = courseCity;
                                golfCourse.state = courseState;
                                courseList.add(golfCourse);
                            }
                        } else {
                            Toast.makeText(context, "Error retrieving the course list.", Toast.LENGTH_LONG).show();
                            Log.w("Start Activity", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 475) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                currentHoleNumber = data.getIntExtra("hole_number", 1);
                currentHoleText.setText("#" + currentHoleNumber);
            }
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long rowId) {

            vibe.vibrate(100);
            String locName = ((TextView) v).getText().toString();

            for (int i = 0; i<courseList.size(); i++) {
                Course golfCourse = courseList.get(i);
                if (golfCourse.name.equalsIgnoreCase(locName)) {

                    Intent intent = new Intent(context, MapsActivity.class);
                    intent.putExtra("course_id", golfCourse.id);
                    intent.putExtra("course_name", golfCourse.name);
                    intent.putExtra("course_info", golfCourse);
//                    intent.putExtra("course_id", courseIdDictionary.get(locName));
//                    intent.putExtra("course_name", locName);
                    startActivity(intent);
                    break;
                }
            }
        }
    };



    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }
    @Override
    public void onConnectionSuspended(int i) {
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("Mobile Golf ACE", "connection suspended");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        MapsActivity.ErrorDialogFragment dialogFragment = new MapsActivity.ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }
}