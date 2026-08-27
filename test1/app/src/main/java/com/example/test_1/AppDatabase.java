package com.example.test_1;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Country.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CountryDao countryDao();
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "country_db")
                    .allowMainThreadQueries() // Dozvoljeno za kolokvijume radi jednostavnosti
                    .build();
        }
        return instance;
    }
}