package com.dejongdevelopment.golfps.sql;

import android.provider.BaseColumns;

/**
 * Created by gdejong on 5/8/17.
 */

public final class StrokeDB {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private StrokeDB() {}

    /* Inner class that defines the table contents */
    public static class StrokeDBEntry implements BaseColumns {
        public static final String TABLE_NAME = "strokes";
        public static final String COLUMN_COURSE_ID = "course_id";
        public static final String COLUMN_HOLE_NUMBER = "hole_number";
        public static final String COLUMN_S_NUM = "stroke_number";
        public static final String COLUMN_S_LOC = "stroke_location";
    }
}

