package com.example.test_1;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "countries")
public class Country {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Pretpostavljamo da JSON sa API-ja ima polje "name" (prilagodi ako ima i druga polja)
    public String name;
}