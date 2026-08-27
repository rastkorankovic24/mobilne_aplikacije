package com.example.popravni;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("roles/{id}")
    Call<Role> getRoleById(@Path("id") int id);
}