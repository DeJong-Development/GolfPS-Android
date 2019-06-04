package com.dejongdevelopment.golfps.services;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * Created by gdejong on 5/8/17.
 */

public class GolfListenerService extends WearableListenerService {

    private static final String TAG = "GolfListenerService";
    private static final String GOING_TO_NEXT_HOLE = "/data-item-received";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents);
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        for (DataEvent event : dataEvents) {
            // DataItem changed
            DataItem item = event.getDataItem();
            if (item.getUri().getPath().compareTo("/ydstopin") == 0) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                int yardsToPin = dataMap.getInt("YARDTOPIN");
                Log.d("GOLF ACE","watch yards to pin: " + yardsToPin);
            } else if (item.getUri().getPath().compareTo("/new_stroke_number") == 0) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
//                numStrokes = dataMap.getInt("STROKENUM");
//                updateNumberOfStrokes(false);
            } else if (item.getUri().getPath().compareTo("/go_to_next_hole") == 0) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
//                currentHoleNumber = dataMap.getInt("HOLENUM");
//                goToCurrentHole();
            }
        }
    }
}
