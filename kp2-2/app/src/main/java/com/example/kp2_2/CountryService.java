package com.example.kp2_2;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CountryService {
    @GET("countries")
    Call<List<Country>> getCountries();
}
