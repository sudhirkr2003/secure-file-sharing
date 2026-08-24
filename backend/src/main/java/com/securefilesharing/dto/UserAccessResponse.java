package com.securefilesharing.dto;

public class UserAccessResponse {

    private String name;
    private String email;

    public UserAccessResponse() {
    }

    public UserAccessResponse(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
