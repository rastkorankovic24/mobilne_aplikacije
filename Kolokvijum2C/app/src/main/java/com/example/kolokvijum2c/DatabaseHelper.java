package com.example.kolokvijum2c;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "kolokvijum2c.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_COMMENTS = "comments";
    public static final String COL_ID = "_id";
    public static final String COL_COMMENT_ID = "comment_id";
    public static final String COL_POST_ID = "post_id";
    public static final String COL_NAME = "name";
    public static final String COL_EMAIL = "email";
    public static final String COL_BODY = "body";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_COMMENTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_COMMENT_ID + " INTEGER, "
                + COL_POST_ID + " INTEGER, "
                + COL_NAME + " TEXT, "
                + COL_EMAIL + " TEXT, "
                + COL_BODY + " TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        onCreate(db);
    }

    public void insertComments(List<Comment> comments) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Comment comment : comments) {
                ContentValues values = new ContentValues();
                values.put(COL_COMMENT_ID, comment.getId());
                values.put(COL_POST_ID, comment.getPostId());
                values.put(COL_NAME, comment.getName());
                values.put(COL_EMAIL, comment.getEmail());
                values.put(COL_BODY, comment.getBody());
                db.insert(TABLE_COMMENTS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteFirstComment() {
        if (getCommentCount() == 0) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(
                TABLE_COMMENTS,
                new String[]{COL_ID},
                null,
                null,
                null,
                null,
                COL_ID + " ASC",
                "1"
        );
        if (cursor != null && cursor.moveToFirst()) {
            long rowId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            cursor.close();
            db.delete(TABLE_COMMENTS, COL_ID + "=?", new String[]{String.valueOf(rowId)});
            return true;
        }
        if (cursor != null) {
            cursor.close();
        }
        return false;
    }

    public int getCommentCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COMMENTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public String getFirstCommentName() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_COMMENTS,
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
