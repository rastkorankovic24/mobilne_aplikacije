package com.example.kolokvijum2d;

import com.google.gson.annotations.SerializedName;

public class Post {

    private int userId;
    private int id;
    private String title;
    private String body;
    private String link;

    @SerializedName("comment_count")
    private int commentCount;

    public int getUserId() {
        return userId;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getLink() {
        return link;
    }

    public int getCommentCount() {
        return commentCount;
    }
}
