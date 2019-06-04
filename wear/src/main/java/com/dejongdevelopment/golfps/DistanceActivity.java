package com.dejongdevelopment.golfps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class DistanceActivity extends WearableActivity implements
        MessageClient.OnMessageReceivedListener,
        DataClient.OnDataChangedListener,
        CapabilityClient.OnCapabilityChangedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);

    private static final String TAG = "Wear";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 45;
    private static final String LOAD_COURSE = "/load-course";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String START_RECEIVED_ACTIVITY = "/start-received-activity";
    private static final String JUST_CONNECTED = "/just-connected";

    private boolean mLocationPermissionGranted = false;

    private boolean waitingToSync = true;

    private Context context = this;

    private BoxInsetLayout mContainerView;
    private TextView distanceTV;
    private TextView mClockView;
    private Button swingButton;
    private Button holeOutButton;
    private TextView unitText;
    private TextView holeNumberText;
    private TextView strokeNumberText;

    private GoogleApiClient mGoogleApiClient;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;


    private int strokeNumber = 0;
    private int holeNumber = 1;
    private Location mCurrentLocation;
    private LatLng mCurrentHolePin;
    private LatLng defaultGeneralLocation;
    private int yardsToPin; //updated from phone or local GPS

    private ArrayList<LatLng> courseHoleInfoList = new ArrayList<LatLng>();


    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) mGoogleApiClient.connect();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) mGoogleApiClient.disconnect();
    }

    @Override
    public void onPause() {
        super.onPause();

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Resuming watch activity");

        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(
                        this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance);
        setAmbientEnabled();

        swingButton = (Button) findViewById(R.id.swingButton);
        holeOutButton = (Button) findViewById(R.id.holeOutButton);
        unitText = (TextView) findViewById(R.id.unitText);
        holeNumberText = (TextView) findViewById(R.id.holeNumberText);
        strokeNumberText = (TextView) findViewById(R.id.strokeText);

        strokeNumberText.setVisibility(View.GONE);
        swingButton.setVisibility(View.GONE);
        swingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            strokeNumber++;
            strokeNumberText.setText("" + strokeNumber);
            updateDataMap(1);
            }
        });
        holeOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            strokeNumber = 0;
            holeNumber++;

            strokeNumberText.setText("-");
            holeNumberText.setText("#" + holeNumber);

            updateDistanceText();
            updateDataMap(1);
            updateDataMap(2);
            }
        });

        
        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        distanceTV = (TextView) findViewById(R.id.distanceText);
        mClockView = (TextView) findViewById(R.id.clock);
        mClockView.setTextColor(Color.WHITE);

        int MIN_BANDWIDTH_KBPS = 320;
        mConnectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = mConnectivityManager.getActiveNetwork();

        if (activeNetwork != null) {
            int bandwidth =
                    mConnectivityManager.getNetworkCapabilities(activeNetwork).getLinkDownstreamBandwidthKbps();

            if (bandwidth < MIN_BANDWIDTH_KBPS) {
                mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        Log.d(TAG, "found new network");
//                if (bindProcessToNetwork(network)) {
//                    // socket connections will now use this network
//                } else {
//                    // app doesn't have android.permission.INTERNET permission
//                }
                    }
                };

                //Request a high-bandwidth network
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                mConnectivityManager.requestNetwork(request, mNetworkCallback);
            }
        } else {
            // You already are on a high-bandwidth network, so start your network request
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.d(TAG, "build google api client");
    }


    private void updateDataMap(int type) {
        String path = "";
        String key;
        int num;
        switch (type) {
            case 1: path = "/new_stroke_number"; break;
            case 2: path = "/go_to_next_hole"; break;
        }
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        switch (type) {
            case 1:
                key = "STROKENUM";
                num = strokeNumber;
                putDataMapReq.getDataMap().putInt(key, num);
                break;
            case 2:
                key = "HOLENUM";
                num = holeNumber;
                putDataMapReq.getDataMap().putInt(key, num);
                break;
        }
        putDataMapReq.getDataMap().putLong("time", System.currentTimeMillis());
        putDataMapReq.getDataMap().putString("device", "watch");
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        Task<DataItem> dataItemTask =
                Wearable.getDataClient(getApplicationContext()).putDataItem(putDataReq);

        new ListenToMobileDataTask().execute(dataItemTask);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals(LOAD_COURSE)){
//            Toast.makeText(context, "Loading course...", Toast.LENGTH_SHORT).show();
            //String courseId = new String(messageEvent.getData(), Charset.forName("utf-8")); // for UTF-8 encoding
            syncDataItems();
        } else if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            // Get the node id from the host value of the URI
            String nodeId = messageEvent.getSourceNodeId();

            // Tell the phone that the activity was already running and is responding
            // "Hey if you want to start, we are waiting for course information!"
            new SendMobileMessageTask().execute(START_RECEIVED_ACTIVITY);
        }
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }
    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            swingButton.setVisibility(View.GONE);
            holeOutButton.setVisibility(View.INVISIBLE);
            unitText.setVisibility(View.INVISIBLE);

            mClockView.setVisibility(View.VISIBLE);
            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
//            swingButton.setVisibility(View.VISIBLE);
            holeOutButton.setVisibility(View.VISIBLE);
            unitText.setVisibility(View.VISIBLE);

            mClockView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);

        Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        //Ask phone if there is any up to date information
        new SendMobileMessageTask().execute(JUST_CONNECTED);

        //no matter what sync items on connect
        if (mCurrentLocation != null) {
            syncDataItems();
        } else {
            waitingToSync = true;
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    //Method for getting all data items in buffer... not sure how it stores all of them...
    private void syncDataItems() {
        Task<DataItemBuffer> itemsTask = Wearable.getDataClient(getApplicationContext()).getDataItems();
        new SyncDataTask().execute(itemsTask);

        Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
        Toast.makeText(context, "Connection failure!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                if (item.getUri().getPath().compareTo("/new_stroke_number") == 0) {
                    strokeNumber = dataMap.getInt("STROKENUM");
                    strokeNumberText.setText(String.valueOf(strokeNumber));
                } else if (item.getUri().getPath().compareTo("/go_to_next_hole") == 0) {
                    Toast.makeText(context, "Going to next hole...", Toast.LENGTH_SHORT).show();
                    holeNumber = dataMap.getInt("HOLENUM");
                    strokeNumber = 0;
                    strokeNumberText.setText("-");
                    holeNumberText.setText("#" + holeNumber);

                    updateDistanceText();
                } else if (item.getUri().getPath().compareTo("/hole_locations") == 0) {
                    Toast.makeText(context, "Getting new hole location information.", Toast.LENGTH_SHORT).show();
                    ArrayList<String> holeInfoList = dataMap.getStringArrayList("HOLELOCATIONS");
                    for (String loc : holeInfoList) {
                        String[] elements = loc.split(":");
                        courseHoleInfoList.add(new LatLng(Double.parseDouble(elements[1]), Double.parseDouble(elements[2])));
                    }
                } else if (item.getUri().getPath().compareTo("/ydstopin") == 0) {
                    Log.d("WATCH - PHONE update: ", "getting update from phone location");
                    yardsToPin = dataMap.getInt("YARDTOPIN");
                    distanceTV.setText(String.valueOf(yardsToPin));
                }
            }
        }
    }

    private void updateDistanceText() {
        if (courseHoleInfoList != null && !courseHoleInfoList.isEmpty()) {
            mCurrentHolePin = courseHoleInfoList.get(holeNumber - 1);
        }

        if (mCurrentHolePin != null) {
            //only check this if we know our current location
            yardsToPin = (int) distanceFromMyLocationTo(mCurrentHolePin);
            distanceTV.setText(String.valueOf(yardsToPin));
        } else {
            distanceTV.setText("-");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Watch Location", "location: " + location.toString());
        mCurrentLocation = location;
        updateDistanceText();

        if (waitingToSync) {
            syncDataItems();
            waitingToSync = false;
        }
    }

    private double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    private double distanceFromMyLocationTo(LatLng otherLocation) {
        double earthRadiusKm = 6371;

        if (mCurrentLocation != null) {
            double lat1 = mCurrentLocation.getLatitude();
            double lon1 = mCurrentLocation.getLongitude();
            double lat2 = otherLocation.latitude;
            double lon2 = otherLocation.longitude;

            double dLat = degreesToRadians(lat2 - lat1);
            double dLon = degreesToRadians(lon2 - lon1);

            lat1 = degreesToRadians(lat1);
            lat2 = degreesToRadians(lat2);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return earthRadiusKm * c * 1093.61; //convert km to yards
        } else {
            return 0;
        }
    }


    // ------------ GET DATA FROM MOBILE DEVICE --------------- //
    private class SyncDataTask extends AsyncTask<Task<DataItemBuffer>, Void, DataItemBuffer> {
        @Override
        protected DataItemBuffer doInBackground(Task<DataItemBuffer>... args) {
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                DataItemBuffer resultItems = Tasks.await(args[0]);
                return resultItems;
            } catch (ExecutionException exception) {
                Log.e("TAG", "Task failed: " + exception);
            } catch (InterruptedException exception) {
                Log.e("TAG", "Interrupt occurred: " + exception);
            }
            return null;
        }

        @Override
        protected void onPostExecute(DataItemBuffer resultItems) {
            if (resultItems.getStatus().isSuccess()) {

                long strokeNumTime = 0;
                long holeNumTime = 0;
                long holeLocationsTime = 0;

                for (DataItem item : resultItems) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String deviceType = dataMap.getString("device");
                    long time = dataMap.getLong("time");
                    if (item.getUri().getPath().compareTo("/new_stroke_number") == 0 && time > strokeNumTime) {
                        strokeNumber = dataMap.getInt("STROKENUM");
                        strokeNumberText.setText(String.valueOf(strokeNumber));
                        strokeNumTime = time;
                    } else if (item.getUri().getPath().compareTo("/go_to_next_hole") == 0 && time > holeNumTime) {
                        holeNumber = dataMap.getInt("HOLENUM");
                        strokeNumber = 0;
                        strokeNumberText.setText("-");
                        holeNumberText.setText("#" + holeNumber);
                        holeNumTime = time;
                    } else if (item.getUri().getPath().compareTo("/hole_locations") == 0 && time > holeLocationsTime) {
                        courseHoleInfoList.clear();
                        ArrayList<String> holeInfoList = dataMap.getStringArrayList("HOLELOCATIONS");
                        for (String loc : holeInfoList) {
                            String[] elements = loc.split(":");
                            courseHoleInfoList.add(new LatLng(Double.parseDouble(elements[1]), Double.parseDouble(elements[2])));
                        }
                        holeLocationsTime = time;
                    }
                }
                updateDistanceText();
            } else {
                Toast.makeText(context, "Syncing failure!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // ------------ SEND DATA TO MOBILE DEVICE --------------- //
    private class ListenToMobileDataTask extends AsyncTask<Task<DataItem>, Void, Void> {
        @Override
        protected Void doInBackground(Task<DataItem>... args) {
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                DataItem dataItem = Tasks.await(args[0]);

                Log.e("TAG", "DataItem saved: " + dataItem);
            } catch (ExecutionException exception) {
                Log.e("TAG", "Task failed: " + exception);
            } catch (InterruptedException exception) {
                Log.e("TAG", "Interrupt occurred: " + exception);
            }
            return null;
        }
    }

    // ------------ SEND MESSAGES TO MOBILE DEVICE --------------- //
    private class SendMobileMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendMobileMessage(node, args[0]);
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
    private void sendMobileMessage(String node, String msg) {
        byte[] data = new byte[0];

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


}
