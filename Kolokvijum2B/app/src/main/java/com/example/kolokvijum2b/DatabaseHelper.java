package com.example.kolokvijum2b;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "kolokvijum2b.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "_id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_NAME = "name";
    public static final String COL_COMPANY = "company";
    public static final String COL_USERNAME = "username";
    public static final String COL_EMAIL = "email";
    public static final String COL_ADDRESS = "address";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_ID + " INTEGER, "
                + COL_NAME + " TEXT, "
                + COL_COMPANY + " TEXT, "
                + COL_USERNAME + " TEXT, "
                + COL_EMAIL + " TEXT, "
                + COL_ADDRESS + " TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public void insertUsers(List<User> users) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (User user : users) {
                ContentValues values = new ContentValues();
                values.put(COL_USER_ID, user.getId());
                values.put(COL_NAME, user.getName());
                values.put(COL_COMPANY, user.getCompany());
                values.put(COL_USERNAME, user.getUsername());
                values.put(COL_EMAIL, user.getEmail());
                values.put(COL_ADDRESS, user.getAddress());
                db.insert(TABLE_USERS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Briše poslednji red (_id DESC)
    public boolean deleteLastUser() {
        if (getUserCount() == 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_ID},
                null,
                null,
                null,
                null,
                COL_ID + " DESC",
                "1"
        );

        if (cursor != null && cursor.moveToFirst()) {
            long rowId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            cursor.close();
            db.delete(TABLE_USERS, COL_ID + "=?", new String[]{String.valueOf(rowId)});
            return true;
        }

        if (cursor != null) {
            cursor.close();
        }
        return false;
    }

    public int getUserCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // Ime prvog korisnika u tabeli (_id ASC)
    public String getFirstUserName() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_NAME},
                null,
                null,
                null,
                null,
                COL_ID + " ASC",
                "1"
        );

        String name = null;
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
            cursor.close();
        }
        return name;
    }
}
