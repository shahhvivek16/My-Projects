package com.example.jigar.booyahproject.rest.model.request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to make POST and PUT requests for sending BooYah! ratings to the server.
 */
public class RatingsRequest implements Serializable {

    @SerializedName("seconds")
    public List<Integer> seconds;

    public RatingsRequest(){
        this(new ArrayList<Integer>());
    }

    public RatingsRequest(List<Integer> seconds){
        this.seconds = seconds;
    }
}
