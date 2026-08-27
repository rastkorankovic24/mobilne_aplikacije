package com.example.test_1;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CountryDao {
    @Insert
    void insertAll(List<Country> countries);

    @Query("SELECT * FROM countries")
    List<Country> getAll();

    @Query("SELECT * FROM countries ORDER BY id DESC LIMIT 1")
    Country getLastCountry();

    @Query("DELETE FROM countries WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT COUNT(*) FROM countries")
    int getCount();
}