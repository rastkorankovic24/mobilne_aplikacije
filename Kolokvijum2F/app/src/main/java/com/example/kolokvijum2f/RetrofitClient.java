package com.example.kolokvijum2f;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://dummy-json.mock.beeceptor.com/";

    private static CountryService countryService;

    public static CountryService getCountryService() {
        if (countryService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            countryService = retrofit.create(CountryService.class);
        }
        return countryService;
    }
}
