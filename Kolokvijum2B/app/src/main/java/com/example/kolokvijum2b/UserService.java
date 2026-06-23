package com.example.kolokvijum2b;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

// Retrofit interfejs za users API
public interface UserService {

    @GET("users")
    Call<List<User>> getUsers();
}
