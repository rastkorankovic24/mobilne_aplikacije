package com.example.kp2_2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "kp2_2.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_COUNTRIES = "countries";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_CODE = "code";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_COUNTRIES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT, "
                + COL_CODE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTRIES);
        onCreate(db);
    }

    public void insertCountries(List<Country> countries) {
        SQLiteDatabase db = getWritableDatabase();
        for (Country country : countries) {
            ContentValues values = new ContentValues();
            values.put(COL_NAME, country.getName());
            values.put(COL_CODE, country.getCode());
            db.insert(TABLE_COUNTRIES, null, values);
        }
    }

    public void deleteLastCountry() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_COUNTRIES
                + " WHERE " + COL_ID + " = (SELECT MAX(" + COL_ID + ") FROM " + TABLE_COUNTRIES + ")");
    }

    public int getCountryCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COUNTRIES, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}
