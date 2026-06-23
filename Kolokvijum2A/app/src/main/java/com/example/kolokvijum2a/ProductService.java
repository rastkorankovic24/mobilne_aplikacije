package com.example.kolokvijum2a;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ProductService {

    @GET("posts")
    Call<List<Product>> getProducts();
}
