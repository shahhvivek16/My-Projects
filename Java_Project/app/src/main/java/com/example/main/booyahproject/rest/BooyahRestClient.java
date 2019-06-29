package com.example.jigar.booyahproject.rest;

import com.example.jigar.booyahproject.rest.model.response.MediaResponse;

import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BooyahRestClient {

    private static Retrofit retrofit;
    private static final String BASE_API_ENDPOINT = "http://34.201.2.107:5000";
    private static List<MediaResponse> medialist;

    public static Retrofit getRetrofitInstance(){
        if(retrofit == null){
            retrofit = new retrofit2.Retrofit.Builder().baseUrl(BASE_API_ENDPOINT).addConverterFactory(GsonConverterFactory.create()).build();
        }

        return retrofit;
    }
}
