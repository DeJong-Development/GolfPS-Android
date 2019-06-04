package com.dejongdevelopment.golfps.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gdejong on 5/8/17.
 */

public class GolfDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "GolfCourseInfo.db";

    private static final String SQL_COURSE_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + GolfCourseDB.GolfCourseDBEntry.TABLE_NAME + " (" +
                    GolfCourseDB.GolfCourseDBEntry._ID + " TEXT PRIMARY KEY," +
                    GolfCourseDB.GolfCourseDBEntry.COLUMN_NAME + " TEXT," +
                    GolfCourseDB.GolfCourseDBEntry.COLUMN_CITY + " TEXT," +
                    GolfCourseDB.GolfCourseDBEntry.COLUMN_STATE + " TEXT," +
                    GolfCourseDB.GolfCourseDBEntry.COLUMN_GEN_LOC + " TEXT," +
                    GolfCourseDB.GolfCourseDBEntry.COLUMN_HOLE_INFO + " TEXT)";

    private static final String SQL_COURSE_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + GolfCourseDB.GolfCourseDBEntry.TABLE_NAME;


    private static final String SQL_STROKE_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + StrokeDB.StrokeDBEntry.TABLE_NAME + " (" +
                    StrokeDB.StrokeDBEntry._ID + " TEXT PRIMARY KEY," +
                    StrokeDB.StrokeDBEntry.COLUMN_COURSE_ID + " TEXT," +
                    StrokeDB.StrokeDBEntry.COLUMN_HOLE_NUMBER + " NUMBER," +
                    StrokeDB.StrokeDBEntry.COLUMN_S_NUM + " NUMBER," +
                    StrokeDB.StrokeDBEntry.COLUMN_S_LOC + " TEXT)";

    private static final String SQL_STROKE_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + StrokeDB.StrokeDBEntry.TABLE_NAME;

    public GolfDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_COURSE_CREATE_ENTRIES);
        db.execSQL(SQL_STROKE_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_COURSE_DELETE_ENTRIES);
        db.execSQL(SQL_STROKE_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
