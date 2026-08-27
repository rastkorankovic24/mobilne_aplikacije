package com.example.popravni_kolokvijum_2;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("roles/{id}")
    Call<Role> getRole(@Path("id") int id);

    class Factory {
        public static ApiService create() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://app.beeceptor.com/mock-server/dummy-json/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            return retrofit.create(ApiService.class);
        }
    }
}