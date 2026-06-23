package com.example.kolokvijum2a;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "kolokvijum2a.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_PRODUCTS = "products";
    public static final String COL_ID = "_id";
    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_PRICE = "price";
    public static final String COL_BRAND = "brand";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_PRODUCTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PRODUCT_ID + " INTEGER, "
                + COL_TITLE + " TEXT, "
                + COL_DESCRIPTION + " TEXT, "
                + COL_PRICE + " INTEGER, "
                + COL_BRAND + " TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    public void insertProducts(List<Product> products) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Product product : products) {
                ContentValues values = new ContentValues();
                values.put(COL_PRODUCT_ID, product.getId());
                values.put(COL_TITLE, product.getTitle());
                values.put(COL_DESCRIPTION, product.getDescription());
                values.put(COL_PRICE, product.getPrice());
                values.put(COL_BRAND, product.getBrand());
                db.insert(TABLE_PRODUCTS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int getProductCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int countProductsByTitle(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return getProductCount();
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_PRODUCTS,
                new String[]{"COUNT(*)"},
                COL_TITLE + " LIKE ? COLLATE NOCASE",
                new String[]{"%" + searchText.trim() + "%"},
                null,
                null,
                null
        );
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public String getFirstProductTitle() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_PRODUCTS,
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

    public boolean deleteFirstProduct() {
        if (getProductCount() == 0) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(
                TABLE_PRODUCTS,
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
            db.delete(TABLE_PRODUCTS, COL_ID + "=?", new String[]{String.valueOf(rowId)});
            return true;
        }
        if (cursor != null) {
            cursor.close();
        }
        return false;
    }
}
