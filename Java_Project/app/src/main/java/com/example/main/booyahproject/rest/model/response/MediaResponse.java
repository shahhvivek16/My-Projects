package com.example.jigar.booyahproject.rest.model.response;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;

import java.io.Serializable;
import java.io.StringReader;
import java.util.Arrays;

public class MediaResponse implements Serializable {

    private final String[] ALLOWED_MTYPES = new String[]{"movie", "music", "television"};

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("duration")
    private int duration;

    @SerializedName("author")
    private String author;

    @SerializedName("mtype")
    private String mtype;

    @SerializedName("indexed")
    private Boolean indexed;

    public MediaResponse(int id, String name, int duration, String author, String mtype, Boolean indexed){
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.author = author;
        this.mtype = mtype;
        this.indexed = indexed;
    }

    public int getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public int getDuration(){
        return this.duration;
    }

    public String getAuthor(){
        return this.author;
    }

    public String getMtype(){
        return this.mtype;
    }

    public boolean isIndexed(){
        return indexed;
    }

    public static MediaResponse createFromObject(Object obj) throws MalformedJsonException {
        String[] arr = obj.toString().split(",");
        for (int i =0 ; i < arr.length; i++){
            if(arr[i].contains("name=") || arr[i].contains("author=") || arr[i].contains("mtype=")){
                String[] split = arr[i].split("=");
                arr[i] = split[0] + "='" + split[1] + "'";
            }
        }
        String json = String.join(",", Arrays.asList(arr));
        JsonReader reader = new JsonReader(new StringReader(json));
        return new Gson().fromJson(reader, MediaResponse.class);
    }
}
