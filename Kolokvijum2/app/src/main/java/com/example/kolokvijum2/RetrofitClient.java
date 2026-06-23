package com.example.kolokvijum2;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Pravi API endpoint (ne stranica dokumentacije sa beeceptor.com/mock-server/...)
    private static final String BASE_URL = "https://dummy-json.mock.beeceptor.com/";

    private static Retrofit retrofit;

    public static PostService getPostService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(PostService.class);
    }
}
