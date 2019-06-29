package com.example.jigar.booyahproject.rest.model.request;

import com.google.gson.annotations.SerializedName;

public class AuthTokenRequest {

    @SerializedName("signature")
    private String signature;

    public AuthTokenRequest(String signature){
        this.signature = signature;
    }
}
