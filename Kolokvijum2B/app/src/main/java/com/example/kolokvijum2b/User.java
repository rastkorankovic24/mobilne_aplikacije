package com.example.kolokvijum2b;

// Model korisnika iz JSON API-ja
public class User {

    private int id;
    private String name;
    private String company;
    private String username;
    private String email;
    private String address;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCompany() {
        return company;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }
}
