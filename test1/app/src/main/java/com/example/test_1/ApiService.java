package com.example.test_1;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    @GET("mock-server/dummy-json")
    Call<List<Country>> getCountries();
}