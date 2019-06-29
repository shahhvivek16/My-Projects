package com.example.jigar.booyahproject.rest.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class StatusResponse {

    @SerializedName("uuid")
    private UUID uuid;

    public StatusResponse(String uuid){
        this.uuid = UUID.fromString(uuid);
    }

    public UUID getUuid(){
        return this.uuid;
    }
}
