package com.dejongdevelopment.golfps.tasks;

import android.os.AsyncTask;

import com.dejongdevelopment.golfps.tools.MapTools;
import com.google.android.gms.maps.model.LatLng;

public class GetElevationTask extends AsyncTask<LatLng, Void, Double> {
    @Override
    protected Double doInBackground(LatLng... args) {
        double elevation = MapTools.getElevation(args[0]);
        return elevation;
    }
}
