package com.example.jigar.booyahproject.rest.model.request;

import com.google.gson.annotations.SerializedName;

public class CreateMediaRequest {

    private final transient String[] ALLOWED_MTYPES = new String[]{"movie", "music", "television"};

    @SerializedName("name")
    private String name;

    @SerializedName("duration")
    private int duration;

    @SerializedName("author")
    private String author;

    @SerializedName("mtype")
    private String mtype;

    @SerializedName("url")
    private String url;

    public CreateMediaRequest(String name, int duration, String author, String mtype, String url){
        this.name = name;
        this.duration = duration;
        this.author = author;
        this.mtype = mtype;
        this.url = url;
    }

    public CreateMediaRequest(String name, int duration, String author, String mtype){
        this(name, duration, author, mtype, null);
    }

}
