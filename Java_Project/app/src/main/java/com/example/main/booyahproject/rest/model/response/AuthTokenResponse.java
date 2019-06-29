package com.example.jigar.booyahproject.rest.model.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class AuthTokenResponse implements Serializable {

    @SerializedName("uid")
    private int id;

    @SerializedName("access_token")
    private String token;

    public AuthTokenResponse(int id, String token){
        this.id = id;
        this.token = token;
    }

    public int getId(){
        return this.id;
    }

    public String getToken(){
        return "Bearer " + this.token;
    }
}
