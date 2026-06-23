package com.example.kolokvijum2a;

import com.google.gson.annotations.SerializedName;

public class Product {

    private int id;
    private String title;

    @SerializedName("body")
    private String description;

    @SerializedName("comment_count")
    private int price;

    @SerializedName("link")
    private String brand;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public String getBrand() {
        return brand;
    }
}
