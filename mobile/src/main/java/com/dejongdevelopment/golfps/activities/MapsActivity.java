package com.dejongdevelopment.golfps.activities;

import android.Manifest;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dejongdevelopment.golfps.R;
import com.dejongdevelopment.golfps.activities.fragments.MyClubsFragment;
import com.dejongdevelopment.golfps.models.Course;
import com.dejongdevelopment.golfps.models.Hole;
import com.dejongdevelopment.golfps.navigation.NavDrawerAdapter;
import com.dejongdevelopment.golfps.navigation.NavDrawerItem;
import com.dejongdevelopment.golfps.navigation.NavMenuItem;
import com.dejongdevelopment.golfps.navigation.NavMenuSection;
import com.dejongdevelopment.golfps.pickers.HoleNumberPicker;
import com.dejongdevelopment.golfps.tasks.ListenToWearableDataTask;
import com.dejongdevelopment.golfps.tools.MapTools;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;


public class MapsActivity extends FragmentActivity implements
        MessageClient.OnMessageReceivedListener,
        DataClient.OnDataChangedListener,
        CapabilityClient.OnCapabilityChangedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private Context context = this;
    private SharedPreferences prefs;
    private Vibrator vibe;

    private Course selectedCourse;
    private Hole currentHole = null;
    private ArrayList<Polyline> distanceLines = new ArrayList<>();

    public SharedPreferences getPrefs() {
        return prefs;
    }
    private static final String DIALOG_ERROR = "dialog_error";
    private static final int REQUEST_RESOLVE_ERROR = 56;
    private static final String LOAD_COURSE = "/load-course";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String START_RECEIVED_ACTIVITY = "/start-received-activity";
    private static final String START_RECEIVED_BACKGROUND = "/start-received-background";
    private static final String JUST_CONNECTED = "/just-connected";
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 16;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean mLocationPermissionGranted = false;

    private Location mCurrentLocation; //the place we are at
    private LatLng currentPinLatLng; //the location of the pin

    private boolean waitingForLocation = false;

    private Marker currentPinMarker;
    private Marker currentTeeMarker;
    private ArrayList<Marker> currentBunkerMarkers = new ArrayList<>();

    private int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 16;

    private Boolean moveStart = true;

    private ActionBarDrawerToggle mDrawerToggle;
    private Button holeNumberButton;
    private TextView distanceToPinTV;
    private TextView distanceToTeeTV;
    private TextView courseNameTV;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private SupportMapFragment mapFragment;
    private Marker clickedMarker; //currently clicked marker up for consideration

    private int currentHoleNumber = 1;

    private boolean mResolvingError = false;
    private String[] navItems;

    private boolean waitingToMoveToHole = false;
    private boolean mapReady = false;
    /**
     * Marker used to mark variable distance on map; help with distances to bunkers, water, safe zones
     */
    private Marker distanceMarker;
    private Polyline lineToMyLocation;
    private Polyline lineToTee;

    /**
     * Handle the states of watch. If we told it to start but watch was already running, etc.
     */
    private boolean watchAlreadyStarted = false;
    private boolean appNeedsSync = false;

    private boolean alignToHole = true;

    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }
    }
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(
                        this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);

        appNeedsSync = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_maps);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        prefs = this.getSharedPreferences("com.flummoxedcosmos.golface", Context.MODE_PRIVATE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);
        navItems = new String[]{"Start New Round", "Map", "My Clubs"};
        NavDrawerItem[] menuItems = new NavDrawerItem[]{
                NavMenuSection.create(100, "Play Golf"),
                NavMenuItem.create(101, navItems[0], "", true, context),
                NavMenuItem.create(102, navItems[1], "", true, context),
                NavMenuSection.create(200, "Settings"),
                NavMenuItem.create(201, navItems[2], "", true, context),
        };
        NavDrawerAdapter navAdapt = new NavDrawerAdapter(context, R.layout.drawer_list_item, menuItems);
        mDrawerList.setAdapter(navAdapt);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.open, R.string.close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = new SupportMapFragment();
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        String selectedCourseId = getIntent().getStringExtra("course_id");
        String selectedCourseName = getIntent().getStringExtra("course_name");
        selectedCourse = getIntent().getParcelableExtra("course_info");

        distanceToPinTV = findViewById(R.id.distanceToPin);
        distanceToTeeTV = findViewById(R.id.suggestedClub);
        courseNameTV = findViewById(R.id.courseName);

        courseNameTV.setText(selectedCourseName);

        Button nextHoleButton = findViewById(R.id.nextButton);
        nextHoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibe.vibrate(100);
                currentHoleNumber++;
                goToCurrentHole();
                updateDataMap(2);
            }
        });
        Button prevHoleButton = findViewById(R.id.previousButton);
        prevHoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibe.vibrate(100);
                currentHoleNumber--;
                goToCurrentHole();
                updateDataMap(2);
            }
        });

        holeNumberButton = findViewById(R.id.holeNumberButton);
        holeNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibe.vibrate(100);
                Intent intent = new Intent(context, HoleNumberPicker.class);
                intent.putExtra("current_hole_number", currentHoleNumber);
                intent.putExtra("maxnumberofholes", selectedCourse.holeInfo.size());
                startActivityForResult(intent, 475);
            }
        });

        ToggleButton alignHole = findViewById(R.id.alignHoleButton);
        alignHole.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alignToHole = isChecked;
            }
        });

        if (selectedCourseId != null) {

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference coursesRef = db.collection("courses");
            coursesRef.document(selectedCourseId).collection("holes")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {

                                selectedCourse.holeInfo.clear();
                                Hole hole = null;

                                for (DocumentSnapshot document : task.getResult()) {
                                    Integer holeNumber = Integer.parseInt(document.getId());

                                    hole = new Hole(holeNumber);

                                    ArrayList<GeoPoint> bunkerInfo = new ArrayList<>();
                                    Object bunkerInfoObj = document.get("bunkers");
                                    if (bunkerInfoObj != null) {
                                        if (bunkerInfoObj instanceof ArrayList) {
                                            bunkerInfo = (ArrayList<GeoPoint>) bunkerInfoObj;
                                        } else if (bunkerInfoObj instanceof GeoPoint) {
                                            bunkerInfo.add((GeoPoint) bunkerInfoObj);
                                        }
                                    }
                                    ArrayList<LatLng> bunkerLocationsForHole = new ArrayList<>();
                                    for (GeoPoint bunker : bunkerInfo) {
                                        LatLng bunkerLocation = new LatLng(bunker.getLatitude(), bunker.getLongitude());
                                        bunkerLocationsForHole.add(bunkerLocation);
                                    }
                                    hole.bunkerLocations = bunkerLocationsForHole;

                                    ArrayList<GeoPoint> teeInfo = new ArrayList<>();
                                    Object teeInfoObj = document.get("tee");
                                    if (teeInfoObj != null) {
                                        if (teeInfoObj instanceof ArrayList) {
                                            teeInfo = (ArrayList<GeoPoint>) teeInfoObj;
                                        } else if (teeInfoObj instanceof GeoPoint) {
                                            teeInfo.add((GeoPoint) teeInfoObj);
                                        }
                                    }
                                    ArrayList<LatLng> teeLocationsForHole = new ArrayList<>();
                                    for (GeoPoint tee : teeInfo) {
                                        LatLng teeLocation = new LatLng(tee.getLatitude(), tee.getLongitude());
                                        teeLocationsForHole.add(teeLocation);
                                    }
                                    hole.teeLocations = teeLocationsForHole;

                                    GeoPoint pin = (GeoPoint) document.get("pin");
                                    LatLng holeLocation = new LatLng(pin.getLatitude(), pin.getLongitude());
                                    hole.pinLocation = holeLocation;

                                    selectedCourse.holeInfo.add(hole);
                                }

                                if (!selectedCourse.holeInfo.isEmpty()) {
                                    updateDataMap(2); //make sure datamap knows we are on the first hole
                                    updateDataMap(3);
                                    goToCurrentHole();
                                } else {
                                    Toast.makeText(context, "Something went wrong in retrieving course from the server.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.w("Start Activity", "Error getting documents.", task.getException());
                                Toast.makeText(context, "Error retrieving the hole information.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(context, "Invalid course id.", Toast.LENGTH_LONG).show();
        }
    }

    private void goToCurrentHole() {
        if (currentHoleNumber > selectedCourse.holeInfo.size()) currentHoleNumber = 1;
        else if (currentHoleNumber <= 0) currentHoleNumber = selectedCourse.holeInfo.size();
        holeNumberButton.setText("#" + currentHoleNumber);

        currentHole = null;
        for (Hole hole : selectedCourse.holeInfo) {
            if (hole.holeNumber == currentHoleNumber) {
                currentHole = hole;
                break;
            }
        }

        if (mapReady) {
            moveCameraToHole(currentHoleNumber, true);
        } else {
            waitingToMoveToHole = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 475) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                currentHoleNumber = data.getIntExtra("hole_number", 1);
                goToCurrentHole();

                updateDataMap(2);
            }
        } else if (requestCode == 689) {
            if (resultCode == RESULT_OK) {
                //remove marker
                clickedMarker.remove();
                Object tag = clickedMarker.getTag();
                if (tag != null) {
                    String markerTag = clickedMarker.getTag().toString();
                    int holeNumber = Integer.parseInt(markerTag.split(":")[0]);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data); //so that it will go to the fragments...
        }
    }

    private void updateDataMap(int type) {
        String path = "";
        String key = "";
        int num = -1;
        switch (type) {
            case 2: path = "/go_to_next_hole"; break;
            case 3: path = "/hole_locations"; break;
        }
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        switch (type) {
            case 2:
                key = "HOLENUM";
                num = currentHoleNumber;
                putDataMapReq.getDataMap().putInt(key, num);
                break;
            case 3:
                //TODO share server information to watch via wearableDataApi
                key = "HOLELOCATIONS";
                ArrayList<String> holeInfoList = new ArrayList<>();
                for (Hole h : selectedCourse.holeInfo) {
                    // get values and store them
                    holeInfoList.add(h.holeNumber + ":" + h.pinLocation.latitude + ":" + h.pinLocation.longitude);
                }
                putDataMapReq.getDataMap().putStringArrayList(key, holeInfoList);
        }
        putDataMapReq.getDataMap().putLong("time", System.currentTimeMillis());
        putDataMapReq.getDataMap().putString("device", "phone");
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        Task<DataItem> dataItemTask =
                Wearable.getDataClient(getApplicationContext()).putDataItem(putDataReq);

        new ListenToWearableDataTask().execute(dataItemTask);
    }

    private void moveCameraToHole(int number, boolean updateMarkers) {
        waitingToMoveToHole = false;

        if (currentPinMarker != null) {
            currentPinMarker.hideInfoWindow();
        }

        if (distanceMarker != null) {
            distanceMarker.remove();
            distanceMarker = null;
        }
        if (lineToMyLocation != null) lineToMyLocation.remove();
        if (lineToTee != null) lineToTee.remove();

        if (currentHole != null) {
            currentPinLatLng = currentHole.pinLocation;

            LatLngBounds bounds = updateMapBoundsForHole(number);
            float zoom = MapTools.getBoundsZoomLevel(bounds, this.mapFragment.getView());
            LatLng center = MapTools.getBoundsCenter(bounds);
            float bearing = MapTools.calcBearing(currentHole.teeLocations.get(0), currentHole.pinLocation) - 20;

            CameraUpdate update = CameraUpdateFactory.newCameraPosition(new CameraPosition(center, zoom, 45f, bearing));
            mMap.animateCamera(update);

            //Don't share yards to pin, instead share latlng and let watch calculate based on current position
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/currentpinlatlng");
            putDataMapReq.getDataMap().putDouble("CURRENTPINLAT", currentPinLatLng.latitude);
            putDataMapReq.getDataMap().putDouble("CURRENTPINLONG", currentPinLatLng.longitude);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent();

            Task<DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(putDataReq);
            new ListenToWearableDataTask().execute(dataItemTask);

            if (updateMarkers) {

                updatePinMarker();
                updateTeeMarker();
                updateBunkerMarkers();
                updateDrivingDistanceLines();
            }
        }
    }

    private void updateCameraBearing(GoogleMap googleMap) {
        if ( googleMap == null) return;

        if (currentHole.teeLocations.size() > 0) {
            float bearing = MapTools.calcBearing(currentHole.teeLocations.get(0), currentHole.pinLocation) - 20;

            CameraPosition camPos = CameraPosition
                    .builder(googleMap.getCameraPosition())
                    .tilt(45f)
                    .bearing(bearing)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
        }
    }

    private LatLngBounds updateMapBoundsForHole(int holeNumber) {
        LatLngBounds.Builder llBuild = new LatLngBounds.Builder();
        for (LatLng tee : currentHole.teeLocations) {
            llBuild.include(tee);
        }
        for (LatLng bunker : currentHole.bunkerLocations) {
            llBuild.include(bunker);
        }
        llBuild.include(currentHole.pinLocation);
        return llBuild.build();
    }

    private int getClubDistance(int clubNum) {
        int num = clubNum + 1;
        int distance = 275;
        switch (clubNum) {
            case 0: distance = prefs.getInt("clubdistance" + num, 250); break;
            case 1: distance = prefs.getInt("clubdistance" + num, 230); break;
            case 2: distance = prefs.getInt("clubdistance" + num, 215); break;
            case 3: distance = prefs.getInt("clubdistance" + num, 205); break;
            case 4: distance = prefs.getInt("clubdistance" + num, 192); break;
            case 5: distance = prefs.getInt("clubdistance" + num, 184); break;
            case 6: distance = prefs.getInt("clubdistance" + num, 173); break;
            case 7: distance = prefs.getInt("clubdistance" + num, 164); break;
            case 8: distance = prefs.getInt("clubdistance" + num, 156); break;
            case 9: distance = prefs.getInt("clubdistance" + num, 140); break;
            case 10: distance = prefs.getInt("clubdistance" + num, 130); break;
            case 11: distance = prefs.getInt("clubdistance" + num, 110); break;
            case 12: distance = prefs.getInt("clubdistance" + num, 80); break;
        }
        return distance;
    }
    private String getClubSuggestion(int ydsTo) {
        int[] avgDistances = new int[13];
        String[] clubNames = new String[13];
        for (int i = 1; i<14; i++) {
            switch (i) {
                case 1: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 250);
                    clubNames[i-1] = prefs.getString("clubname" + i, "Driver"); break;
                case 2: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 230);
                    clubNames[i-1] = prefs.getString("clubname" + i, "5 Wood"); break;
                case 3: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 215);
                    clubNames[i-1] = prefs.getString("clubname" + i, "3 Wood"); break;
                case 4: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 205);
                    clubNames[i-1] = prefs.getString("clubname" + i, "3 Iron"); break;
                case 5: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 192);
                    clubNames[i-1] = prefs.getString("clubname" + i, "4 Iron"); break;
                case 6: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 184);
                    clubNames[i-1] = prefs.getString("clubname" + i, "5 Iron"); break;
                case 7: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 173);
                    clubNames[i-1] = prefs.getString("clubname" + i, "6 Iron"); break;
                case 8: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 164);
                    clubNames[i-1] = prefs.getString("clubname" + i, "7 Iron"); break;
                case 9: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 156);
                    clubNames[i-1] = prefs.getString("clubname" + i, "8 Iron"); break;
                case 10: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 140);
                    clubNames[i-1] = prefs.getString("clubname" + i, "9 Iron"); break;
                case 11: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 130);
                    clubNames[i-1] = prefs.getString("clubname" + i, "Pitching Wedge"); break;
                case 12: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 110);
                    clubNames[i-1] = prefs.getString("clubname" + i, "Gap Wedge"); break;
                case 13: avgDistances[i-1] = prefs.getInt("clubdistance" + i, 80);
                    clubNames[i-1] = prefs.getString("clubname" + i, "Sand Wedge"); break;
            }
        }
        int clubNum = 0;
        while (ydsTo < avgDistances[clubNum] && clubNum < 12) { clubNum++; } //iterate until we hit the appropriate club
        return clubNames[clubNum];
    }

    private void updateMyLocationPolyLine() {
        if (lineToMyLocation != null) lineToMyLocation.remove();
        if (lineToTee != null) {
            List<LatLng> lineToTeePoints = lineToTee.getPoints();
            LatLng middlePoint = lineToTeePoints.get(0);

            int ydsTo = MapTools.distanceFrom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                    middlePoint);
            int ydsFrom = MapTools.distanceFrom(middlePoint, currentPinLatLng);

            if (ydsTo < 1000) {
                lineToMyLocation = mMap.addPolyline(new PolylineOptions()
                        .add(middlePoint)
                        .add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                        .width(2)
                        .color(Color.WHITE));
            } else {
                lineToMyLocation = mMap.addPolyline(new PolylineOptions()
                        .add(middlePoint)
                        .add(currentTeeMarker.getPosition())
                        .width(2)
                        .color(Color.WHITE));

                ydsTo = MapTools.distanceFrom(currentTeeMarker.getPosition(), middlePoint);
            }
            String suggestedClub = getClubSuggestion(ydsTo);

            if (distanceMarker != null) {
                distanceMarker.setTitle(ydsTo + " yds");
                distanceMarker.setSnippet(suggestedClub);
            }
        }
    }
    private void updatePolyLines(LatLng latLng) {
        if (lineToTee != null) lineToTee.remove();
        lineToTee = mMap.addPolyline(new PolylineOptions()
                .add(latLng)
                .add(currentPinLatLng)
                .width(2)
                .color(Color.WHITE));
        updateMyLocationPolyLine();
    }
    private void updateTeeMarker() {
        if (currentHole.teeLocations != null && currentHole.teeLocations.size() > 0) {
            LatLng teeLocation = currentHole.teeLocations.get(0);
            if (currentTeeMarker == null && teeLocation != null) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(teeLocation)
                        .title("Tee #" + currentHoleNumber)
                        .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(R.drawable.tee_marker)));
                currentTeeMarker = mMap.addMarker(markerOptions);
                currentTeeMarker.setTag(currentHoleNumber + ":T");
            } else if (teeLocation != null) {
                currentTeeMarker.setPosition(teeLocation);
                currentTeeMarker.setTitle("Tee #" + currentHoleNumber);
                currentTeeMarker.setTag(currentHoleNumber + ":T");
            }
        }
    }
    private void updatePinMarker() {
        int teeYardsToPin = 0;
        if (currentHole.teeLocations != null && currentHole.teeLocations.size() > 0) {
            LatLng teeLocation = currentHole.teeLocations.get(0);
            teeYardsToPin = MapTools.distanceFrom(currentPinLatLng, teeLocation);
        }

//        new GetElevationTask().execute(currentPinLatLng);
//        double pinAltitude = MapTools.getAltitude(currentPinLatLng);
//        double teeAltitude = MapTools.getAltitude(currentHole.teeLocations.get(0));

        if (currentPinMarker == null && currentPinLatLng != null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentPinLatLng)
                    .title("Pin #" + currentHoleNumber)
                    .snippet(teeYardsToPin + " yds")
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(R.drawable.flag_marker)));
            currentPinMarker = mMap.addMarker(markerOptions);
            currentPinMarker.setTag(currentHoleNumber + ":P");
        } else if (currentPinLatLng != null) {
            currentPinMarker.setPosition(currentPinLatLng);
            currentPinMarker.setTitle("Pin #" + currentHoleNumber);
            currentPinMarker.setSnippet(teeYardsToPin + " yds");
            currentPinMarker.setTag(currentHoleNumber + ":P");
        }

        if (mCurrentLocation != null && currentPinLatLng != null) {
            waitingForLocation = false;

            double myYardsToPin = MapTools.distanceFrom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                    currentPinLatLng);
            distanceToPinTV.setText((int) myYardsToPin + " yds");
        } else {
            waitingForLocation = true;
        }
    }
    private void updateBunkerMarkers() {
        for (Marker bunkerMarker : currentBunkerMarkers) {
            bunkerMarker.remove();
        }
        currentBunkerMarkers.clear();

        if (currentHole.bunkerLocations != null) {
            for (int i = 0; i < currentHole.bunkerLocations.size(); i++) {
                LatLng bunkerLocation = currentHole.bunkerLocations.get(i);
                if (bunkerLocation != null) {
                    int yardsToBunker = MapTools.distanceFrom(bunkerLocation, currentTeeMarker.getPosition());
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(bunkerLocation)
                            .title("Bunker")
                            .snippet(yardsToBunker + " yds")
                            .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(R.drawable.hazard_marker)));
                    Marker bunkerMarker = mMap.addMarker(markerOptions);
                    bunkerMarker.setTag(currentHoleNumber + ":B" + i);
                    currentBunkerMarkers.add(bunkerMarker);
                }
            }
        }
    }
    private void updateDrivingDistanceLines() {
        LatLng teeLocation = null;
        if (currentHole.teeLocations != null) {
            teeLocation = currentHole.teeLocations.get(0);
        }
        if (teeLocation != null) {
            LatLng pinLocation = currentHole.pinLocation;
            float bearing = MapTools.calcBearing(teeLocation, pinLocation);

            int[] lineColors = new int[] { Color.RED, Color.MAGENTA, Color.YELLOW };

            for (Polyline line : distanceLines) {
                line.remove();
            }
            distanceLines.clear();

            int teeYardsToPin = MapTools.distanceFrom(currentPinLatLng, teeLocation);
            if (getClubDistance(0) < teeYardsToPin) {
                for (int i = 0; i < 3; i++) {
                    double distance = (double) getClubDistance(i);

                    PolylineOptions lineOptions = new PolylineOptions().width(2).color(lineColors[i]);
                    for (int j = -3; j <= 3; j++) {
                        int angle = 4 * j;
                        LatLng newCoords = MapTools.calculateDistanceCoordinates(teeLocation, distance, (double) bearing + angle);
                        lineOptions.add(newCoords);
                    }
                    Polyline distanceLine = mMap.addPolyline(lineOptions);
                    distanceLines.add(distanceLine);
                }
            }
        }
    }

    private Bitmap resizeMapIcons(int resourceId){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), resourceId);
        return Bitmap.createScaledBitmap(imageBitmap, 150, 150, false);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Mobile Golf ACE", "connection suspended");
        appNeedsSync = true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        }

        new SendWearableMessageTask().execute(START_ACTIVITY_PATH);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                return false;
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //if we click the same marker twice -> check if we want to delete it
//                if (clickedMarker != null && clickedMarker.getTag() == marker.getTag() && clickedMarker != distanceMarker) {
//                    Intent intent = new Intent(context, MarkerClick.class);
//                    startActivityForResult(intent, 689);
//                }
                clickedMarker = marker;
                clickedMarker.showInfoWindow();
                return true;
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //remove lines on map if we just click
                if (distanceMarker != null) {
                    distanceMarker.remove();
                    distanceMarker = null;
                }
                if (lineToMyLocation != null) lineToMyLocation.remove();
                if (lineToTee != null) lineToTee.remove();
            }
        });
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                vibe.vibrate(100);
                int ydsTo = MapTools.distanceFrom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                        latLng);
                if (ydsTo > 1000) {
                    ydsTo = MapTools.distanceFrom(currentTeeMarker.getPosition(), latLng);
                }
                int ydsFrom = MapTools.distanceFrom(latLng, currentPinLatLng);
                String suggestedClub = getClubSuggestion(ydsTo);

                if (distanceMarker != null) {
                    // Marker was not set yet. Add marker:
                    distanceMarker.remove();
                    distanceMarker = null;
                }
                distanceMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(R.drawable.golf_ball_blank)))
                        .title(ydsTo + " yds")
                        .snippet(suggestedClub));
                distanceMarker.setTag("distance_marker");
                distanceMarker.showInfoWindow();
                updatePolyLines(latLng);
            }
        });
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                vibe.vibrate(100);
            }
            @Override
            public void onMarkerDrag(Marker marker) {
                if (marker.getTag() == distanceMarker.getTag()) {
                    LatLng latLng = distanceMarker.getPosition();
                    int ydsFrom = MapTools.distanceFrom(latLng, currentPinLatLng);
                    int ydsTo = MapTools.distanceFrom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                            distanceMarker.getPosition());
                    if (ydsTo > 1000) {
                        ydsTo = MapTools.distanceFrom(currentTeeMarker.getPosition(), latLng);
                    }

                    String suggestedClub = getClubSuggestion(ydsTo);
                    distanceMarker.setTitle(ydsTo + " yds");
                    distanceMarker.setSnippet(suggestedClub);
                    distanceMarker.showInfoWindow();

                    updatePolyLines(latLng);
                }
            }
            @Override
            public void onMarkerDragEnd(Marker marker) {
            }
        });
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mapReady = true;
                if (waitingToMoveToHole) {
                    goToCurrentHole();
                }
            }
        });

    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("Mobile Golf ACE", "connection suspended");
        if (mResolvingError) {
        // Already attempting to resolve an error.
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

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;

        if (moveStart || waitingForLocation) {
            moveCameraToHole(currentHoleNumber, moveStart);
            moveStart = false;
            waitingForLocation = false;
        }

        if (currentPinLatLng != null) {
            double yardsToPin = MapTools.distanceFrom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                    currentPinLatLng);
            distanceToPinTV.setText((int) yardsToPin + " yds");

            String suggestedClub = getClubSuggestion((int) yardsToPin);
            distanceToTeeTV.setText(suggestedClub);

            //TODO if watch has no GPS then share this data... check capability???
            PutDataMapRequest putDataMapReqYds = PutDataMapRequest.create("/ydstopin");
            putDataMapReqYds.getDataMap().putInt("YARDTOPIN", (int)yardsToPin);
            PutDataRequest putDataReqYds = putDataMapReqYds.asPutDataRequest();
            putDataReqYds.setUrgent();

            Task<DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(putDataReqYds);

            new ListenToWearableDataTask().execute(dataItemTask);
        }

        if (lineToMyLocation != null && distanceMarker != null) {
            updateMyLocationPolyLine();
        }
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEvents) {
        //TODO resync with most accurate information? what happens if phone gets disconnected in the middle of the round
        //TODO do we continually store information locally on the device rather than in the application?

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
//                if (item.getUri().getPath().compareTo("/ydstopin") == 0) {
//                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
//                    int yardsToPin = dataMap.getInt("YARDTOPIN");
//                    Log.d("GOLF ACE","watch yards to pin: " + yardsToPin);
//                } else
                if (item.getUri().getPath().compareTo("/go_to_next_hole") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    currentHoleNumber = dataMap.getInt("HOLENUM");
                    goToCurrentHole();
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        //only tell watch to load course after we tell it to start the activity (should take care of all states)
        switch (messageEvent.getPath()) {
            case START_RECEIVED_BACKGROUND:
                Toast.makeText(context, "Watch app launched!", Toast.LENGTH_SHORT).show();
                Log.d("MapsActivity", "watch started, now waiting patiently for its connected call");
                break;
            case START_RECEIVED_ACTIVITY:
                watchAlreadyStarted = true;
                new SendWearableMessageTask().execute(LOAD_COURSE);
                break;
            case JUST_CONNECTED:
                //watch told us that it just connected to google apis, somehow it lost connection so send it everything again...
                new SendWearableMessageTask().execute(LOAD_COURSE);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                        LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {

    }

    // ------------ SEND MESSAGES TO WEAR DEVICE --------------- //
    private class SendWearableMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendWearMessage(node, args[0]);
            }
            return null;
        }
    }
    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        Task<List<Node>> nodeListTask = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            List<Node> nodes = Tasks.await(nodeListTask);
            for (Node node : nodes) {
                results.add(node.getId());
            }
        } catch (ExecutionException exception) {
            Log.e("TAG", "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e("TAG", "Interrupt occurred: " + exception);
        }
        return results;
    }
    @WorkerThread
    private void sendWearMessage(String node, String msg) {
        byte[] data = new byte[0];
        if (msg.equals(LOAD_COURSE)) {
            data = selectedCourse.id.getBytes();
        }

        Task<Integer> sendMessageTask = Wearable.getMessageClient(this).sendMessage(node, msg, data);
        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            Log.d("TAG", "Message sent: " + result);
        } catch (ExecutionException exception) {
            Log.e("TAG", "Task failed: " + exception);
        } catch (InterruptedException exception) {
            Log.e("TAG", "Interrupt occurred: " + exception);
        }
    }



    //// ------------- ERROR CATCHING ----------------- ////
    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
//        private static final String DIALOG_ERROR = "dialog_error";

        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MapsActivity) getActivity()).onDialogDismissed();
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }
    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }



    //// ------------- NAVIGATION DRAWER -------------- ////
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    public final void setSubtitle(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if(actionBar != null) actionBar.setSubtitle(subTitle);
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);

        mDrawerLayout.closeDrawer(mDrawerList);

        if (position == 4) { //my clubs
            setSubtitle(navItems[2]);
            distanceToTeeTV.setVisibility(View.GONE);
            distanceToPinTV.setVisibility(View.GONE);
            courseNameTV.setVisibility(View.GONE);
            mapReady = false;
            currentPinMarker = null;
            currentTeeMarker = null;
            MyClubsFragment fragment = new MyClubsFragment();
            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        } else if (position == 2) {
            setSubtitle(navItems[1]);
            distanceToTeeTV.setVisibility(View.VISIBLE);
            distanceToPinTV.setVisibility(View.VISIBLE);
            courseNameTV.setVisibility(View.VISIBLE);
            if (!mapReady) {
                // Insert the fragment by replacing any existing fragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, mapFragment)
                        .commit();
                mapFragment.getMapAsync(this);
            } else {
                goToCurrentHole();
            }
        } else if (position == 1) {
            setSubtitle(navItems[0]);
            finish(); //go back to start activity
        }
    }
}
