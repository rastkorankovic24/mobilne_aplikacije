package com.example.kolokvijum2c;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CommentService {

    @GET("comments")
    Call<List<Comment>> getComments();
}
