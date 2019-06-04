package com.dejongdevelopment.golfps.sql;

import android.provider.BaseColumns;

/**
 * Created by gdejong on 5/8/17.
 */

public final class GolfCourseDB {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private GolfCourseDB() {}

    /* Inner class that defines the table contents */
    public static class GolfCourseDBEntry implements BaseColumns {
        public static final String TABLE_NAME = "courses";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_CITY = "city";
        public static final String COLUMN_STATE = "state";
        public static final String COLUMN_GEN_LOC = "general_location";
        public static final String COLUMN_HOLE_INFO = "hole_info";
    }
}

