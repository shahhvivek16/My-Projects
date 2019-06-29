package com.example.jigar.booyahproject.rest.model.response;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;

import java.io.Serializable;
import java.io.StringReader;
import java.util.Arrays;

public class RecognitionResponse implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("offset")
    private float offset;

    @SerializedName("duration")
    private int duration;

    @SerializedName("match_time")
    private float matchTime;

    public RecognitionResponse(int id, String name, float offset, int duration, float matchTime){
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.offset = offset;
        this.matchTime = matchTime;
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

    public float getOffset(){
        return this.offset;
    }

    public float getMatchTime(){
        return this.matchTime;
    }

    public static RecognitionResponse createFromObject(Object obj) throws MalformedJsonException {
        // see: https://stackoverflow.com/a/11488385
        String[] arr = obj.toString().split(",");
        for (int i =0 ; i < arr.length; i++){
            if(arr[i].contains("name=")){
                String[] split = arr[i].split("=");
                arr[i] = split[0] + "='" + split[1] + "'";
            }
        }
        String json = String.join(",", Arrays.asList(arr));
        JsonReader reader = new JsonReader(new StringReader(json));
        return new Gson().fromJson(reader, RecognitionResponse.class);
    }
}
