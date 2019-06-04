package com.dejongdevelopment.golfps.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Hole {

    public Hole(int number) {
        this.holeNumber = number;
    }

    public int holeNumber = 1;

    public ArrayList<LatLng> bunkerLocations = new ArrayList<>();
    public ArrayList<LatLng> teeLocations = new ArrayList<>();
    public LatLng pinLocation;
}
