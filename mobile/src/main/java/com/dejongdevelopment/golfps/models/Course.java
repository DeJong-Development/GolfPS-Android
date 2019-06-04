package com.dejongdevelopment.golfps.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Course implements Parcelable {

    public Course(String id) {
        this.id = id;
    }

    public String id = "";
    public String name = "";
    public String city = "";
    public String state = "";

    public ArrayList<Hole> holeInfo = new ArrayList<>();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(city);
        dest.writeString(state);
    }

    public static final Parcelable.Creator<Course> CREATOR
            = new Parcelable.Creator<Course>() {
        public Course createFromParcel(Parcel in) {
            return new Course(in);
        }

        public Course[] newArray(int size) {
            return new Course[size];
        }
    };

    private Course(Parcel in) {
        id = in.readString();
        name = in.readString();
        city = in.readString();
        state = in.readString();
    }
}
