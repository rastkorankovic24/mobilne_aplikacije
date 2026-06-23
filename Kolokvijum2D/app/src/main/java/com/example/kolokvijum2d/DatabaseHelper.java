package com.example.kolokvijum2d;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "kolokvijum2d.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_POSTS = "posts";
    public static final String COL_ID = "_id";
    public static final String COL_POST_ID = "post_id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_TITLE = "title";
    public static final String COL_BODY = "body";
    public static final String COL_LINK = "link";
    public static final String COL_COMMENT_COUNT = "comment_count";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_POSTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_POST_ID + " INTEGER, "
                + COL_USER_ID + " INTEGER, "
                + COL_TITLE + " TEXT, "
                + COL_BODY + " TEXT, "
                + COL_LINK + " TEXT, "
                + COL_COMMENT_COUNT + " INTEGER)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    public long insertPost(Post post) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_POST_ID, post.getId());
        values.put(COL_USER_ID, post.getUserId());
        values.put(COL_TITLE, post.getTitle());
        values.put(COL_BODY, post.getBody());
        values.put(COL_LINK, post.getLink());
        values.put(COL_COMMENT_COUNT, post.getCommentCount());
        return db.insert(TABLE_POSTS, null, values);
    }

    public void insertPosts(List<Post> posts) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Post post : posts) {
                ContentValues values = new ContentValues();
                values.put(COL_POST_ID, post.getId());
                values.put(COL_USER_ID, post.getUserId());
                values.put(COL_TITLE, post.getTitle());
                values.put(COL_BODY, post.getBody());
                values.put(COL_LINK, post.getLink());
                values.put(COL_COMMENT_COUNT, post.getCommentCount());
                db.insert(TABLE_POSTS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteFirstPost() {
        if (getPostCount() == 0) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(
                TABLE_POSTS,
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
            db.delete(TABLE_POSTS, COL_ID + "=?", new String[]{String.valueOf(rowId)});
            return true;
        }
        if (cursor != null) {
            cursor.close();
        }
        return false;
    }

    public int getPostCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_POSTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public String getFirstPostTitle() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_POSTS,
                new String[]{COL_TITLE},
                null,
                null,
                null,
                null,
                COL_ID + " ASC",
                "1"
        );
        String title = null;
        if (cursor != null && cursor.moveToFirst()) {
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
            cursor.close();
        }
        return title;
    }

    public String getFirstPostBody() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_POSTS,
                new String[]{COL_BODY},
                null,
                null,
                null,
                null,
                COL_ID + " ASC",
                "1"
        );
        String body = null;
        if (cursor != null && cursor.moveToFirst()) {
            body = cursor.getString(cursor.getColumnIndexOrThrow(COL_BODY));
            cursor.close();
        }
        return body;
    }
}
