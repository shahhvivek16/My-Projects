package com.example.jigar.booyahproject.rest.model.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Returned in the body of GET requests for existing Booyah! ratings.
 */
public class RatingsResponse implements Serializable {

    @SerializedName("seconds")
    public List<Integer> seconds;

    @SerializedName("user")
    public int user;

    @SerializedName("media")
    public int media;


    public RatingsResponse(int user, int media){
        this(new ArrayList<Integer>(), user, media);
    }

    public RatingsResponse(List<Integer> seconds, int user, int media){
        this.seconds = seconds;
        this.user = user;
        this.media = media;
    }
}
