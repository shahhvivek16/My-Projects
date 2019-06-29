/**
 * Created by Jigar Joshi on 12th Nov 2018
 * This class contains logic to show the charts for the media
 * last edited on 30th Nov 2018
 */

package com.example.jigar.booyahproject;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.example.jigar.booyahproject.rest.model.response.MediaResponse;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.example.jigar.booyahproject.rest.BooyahRestClient;
import com.example.jigar.booyahproject.rest.BooyahRestService;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.response.RatingsResponse;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsActivity extends AppCompatActivity{

    private Intent intent;
    public BooyahRestService getRatingsService;
    public AuthTokenResponse userToken;
    public MediaResponse mediaDetails;
    public TextView userMediaName;
    public TextView aggregateMediaName;
    private FloatingActionButton fabSample;
    private RelativeLayout mediaScreen;
    private TextView errorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        userMediaName=findViewById(R.id.userRatingsTxtView);
        aggregateMediaName=findViewById(R.id.AggregateRatingsTxtView);
        fabSample = findViewById(R.id.fabSample);
        mediaScreen= findViewById(R.id.body);
        errorMsg=findViewById(R.id.errorMsg);

        Toolbar navToolbar = findViewById(R.id.headerNav);
        setSupportActionBar(navToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();

        navToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        fabSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(ReportsActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });

        //Call API
        getRatingsService = BooyahRestClient.getRetrofitInstance().create(BooyahRestService.class);

        //Get Intent
        intent= getIntent();
        Bundle bundle = intent.getExtras();


        mediaDetails=(MediaResponse) bundle.getSerializable(Constants.MEDIA_ITEM);
        userToken = (AuthTokenResponse) bundle.getSerializable(Constants.AUTH_TOKEN); // grab our authToken

        userMediaName.setText("Your rating - "+mediaDetails.getName());
        aggregateMediaName.setText("Average Ratings - "+mediaDetails.getName());

        //call async method
        getUserMediaLikes();

    }

    public void GenerateUserGraph(RatingsResponse ratings,RatingsResponse ratingsDislikes) {
        if(ratings.seconds == null || ratingsDislikes.seconds== null)
        {
             mediaScreen.setVisibility(View.INVISIBLE);
             errorMsg.setVisibility(View.VISIBLE);

        }
        else {
            errorMsg.setVisibility(View.INVISIBLE);
            mediaScreen.setVisibility(View.VISIBLE);
            //Get Seconds from the response

            List<Integer> likes = ratings.seconds;
            HashMap<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < likes.size(); i++) {
                map.put(likes.get(i), 1);
            }

            List<Integer> dislikes = ratingsDislikes.seconds;
            for (int i = 0; i < dislikes.size(); i++) {
                map.put(dislikes.get(i), -1);
            }

            //sort by key
            Map<Integer, Integer> sortMap = new TreeMap<Integer, Integer>(map);

            //To generate graph
            GraphView graph = findViewById(R.id.graph);
            GraphView aggregateGraph = findViewById(R.id.graphAggregate);


            NavigableMap<Integer, Integer> nav = new TreeMap<Integer, Integer>(sortMap);
            DataPoint[] dp = new DataPoint[sortMap.size()];

            //For loop to get N number of DataPoints
            int flag = 0;
            for (Map.Entry<Integer, Integer> entry : sortMap.entrySet()) {
                Integer key = entry.getKey();
                Integer tab = entry.getValue();
                // do something with key and/or tab
                dp[flag] = new DataPoint(key, tab);
                flag++;
            }
            //Add DataPoints to BarGraphSeries
            BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dp);

            //user Ratings
            graph.addSeries(series);
            StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
            staticLabelsFormatter.setVerticalLabels(new String[]{"Boo!", "N", "Yah!"});
            graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(6);

            graph.getViewport().setScrollable(true); // enables horizontal scrolling
            graph.getViewport().setScalable(true);
            graph.getViewport().setScalableY(true);

            series.setColor(Color.rgb(0, 153, 153));

            series.setSpacing(10);

            // draw values on top
            series.setDrawValuesOnTop(true);
            series.setValuesOnTopSize(50);

            //Aggregate ratings
            BarGraphSeries<DataPoint> aggregateDP = new BarGraphSeries<>(new DataPoint[]{
                    new DataPoint(0, -2),
                    new DataPoint(1, 5),
                    new DataPoint(2, 6),
                    new DataPoint(3, -1),
                    new DataPoint(4, 6),
                    new DataPoint(5, 2),
                    new DataPoint(6, 8),
                    new DataPoint(7, 9)
            });

            getAggregateMediaLikes();
        }
    }

    /**
     * User Rating functions
     */
    private void getUserMediaLikes(){
        //TODO: Pass the media when the search media page is ready
        Call<RatingsResponse> serviceCall = getRatingsService.getMediaUserLikes(userToken.getToken(),mediaDetails.getId(),userToken.getId());

        //TODO: Create custom class. OnResponse() should broadcast result to listeners
        serviceCall.enqueue(new Callback<RatingsResponse>() {
            @Override
            public void onResponse(Call<RatingsResponse> serviceCall, Response<RatingsResponse> response) {
                RatingsResponse userLikes=response.body();
                //Get Dislikes
                getUserMediaDislikes(userLikes);
            }

            @Override
            public void onFailure(Call<RatingsResponse> call, Throwable t) {
                Log.e(Constants.LOG_TAG, "Media list retrieval failed.");
            }
        });
    }

    private void getUserMediaDislikes(final RatingsResponse likes) {
        //TODO: Pass the media when the search media page is ready
        Call<RatingsResponse> serviceCall = getRatingsService.getMediaUserDislikes(userToken.getToken(),mediaDetails.getId(),userToken.getId());

        //TODO: Create custom class. OnResponse() should broadcast result to listeners
        serviceCall.enqueue(new Callback<RatingsResponse>() {
            @Override
            public void onResponse(Call<RatingsResponse> serviceCall, Response<RatingsResponse> response) {
                RatingsResponse userDislikes = response.body();
                GenerateUserGraph(likes,userDislikes);
            }

            @Override
            public void onFailure(Call<RatingsResponse> call, Throwable t) {
                Log.e(Constants.LOG_TAG, "Media list retrieval failed.");
            }
        });
    }

    private void getAggregateMediaLikes(){
        //TODO: Pass the media when the search media page is ready
        Call<List<RatingsResponse>> serviceCall = getRatingsService.getMediaLikes(userToken.getToken(),mediaDetails.getId());

        //TODO: Create custom class. OnResponse() should broadcast result to listeners
        serviceCall.enqueue(new Callback<List<RatingsResponse>>() {
            @Override
            public void onResponse(Call<List<RatingsResponse>> serviceCall, Response<List<RatingsResponse>> response) {
                Log.e(Constants.LOG_TAG, "dataLikes"+response.toString());
                List<RatingsResponse> mediaLikes= response.body();
                //Get Dislikes
                getAggregateMediaDislikes(mediaLikes);
            }

            @Override
            public void onFailure(Call<List<RatingsResponse>> call, Throwable t) {
                Log.e(Constants.LOG_TAG, "Media list retrieval failed.");
            }
        });
    }

    private void getAggregateMediaDislikes(final List<RatingsResponse> mediaLikes) {
        //TODO: Pass the media when the search media page is ready
        Call<List<RatingsResponse>> serviceCall = getRatingsService.getMediaDislikes(userToken.getToken(),mediaDetails.getId());

        //TODO: Create custom class. OnResponse() should broadcast result to listeners
        serviceCall.enqueue(new Callback<List<RatingsResponse>>() {
            @Override
            public void onResponse(Call<List<RatingsResponse>> serviceCall, Response<List<RatingsResponse>> response) {

                List<RatingsResponse> userDislikes = (List<RatingsResponse>) response.body();
                Log.e(Constants.LOG_TAG, "dataLikes"+response.toString());

                GenerateAggregateUserGraph(mediaLikes,userDislikes);
            }

            @Override
            public void onFailure(Call<List<RatingsResponse>> call, Throwable t) {
                Log.e(Constants.LOG_TAG, "Media list retrieval failed.");
            }
        });
    }

    public void GenerateAggregateUserGraph(List<RatingsResponse> aggregateLikes,List<RatingsResponse> aggregateDislikes) {

        if(aggregateDislikes.isEmpty() == false && aggregateLikes.isEmpty() == false){
            //call Hashmap object for the collection of key/value pairs
            HashMap<Integer, Integer> map = new HashMap<>();

            //for the same key increase the value by 1 -- Likes
            for (int userIdx = 0; userIdx < aggregateLikes.size(); userIdx++) {
                List<Integer> seconds = aggregateLikes.get(userIdx).seconds;

                for (int secIdx = 0; secIdx < seconds.size(); secIdx++) {

                    Integer key = seconds.get(secIdx);

                    if (map.containsKey(key)) {
                        Integer mapValue = map.get(key);
                        map.replace(key, mapValue + 1);
                    } else {
                        map.put(key, 1);
                    }
                }
            }

            //for the same key decrease the value by 1 -- Dislikes
            for (int userIdx = 0; userIdx < aggregateDislikes.size(); userIdx++) {
                List<Integer> seconds = aggregateDislikes.get(userIdx).seconds;

                for (int secIdx = 0; secIdx < seconds.size(); secIdx++) {

                    Integer key = seconds.get(secIdx);

                    if (map.containsKey(key)) {
                        Integer mapValue = map.get(key);
                        map.replace(key, mapValue - 1);
                    } else {
                        map.put(key, -1);
                    }

                }
            }

            //sort by key
            Map<Integer, Integer> sortMap = new TreeMap<Integer, Integer>(map);

            GraphView aggregateGraph = findViewById(R.id.graphAggregate);

            NavigableMap<Integer, Integer> nav = new TreeMap<Integer, Integer>(sortMap);
            DataPoint[] dp = new DataPoint[sortMap.size()];

            //store each datapoint
            int flag = 0;
            for (Map.Entry<Integer, Integer> entry : sortMap.entrySet()) {
                Integer key = entry.getKey();
                Integer tab = entry.getValue();
                // datapoints
                dp[flag] = new DataPoint(key, tab);
                flag++;
            }

            //Aggregate ratings--add datapoints
            //datapoints in the graph always accepts keys in ascending oreder
            BarGraphSeries<DataPoint> aggregateDP = new BarGraphSeries<>(dp);

            //user Ratings
            aggregateGraph.addSeries(aggregateDP);
            StaticLabelsFormatter staticLabelsFormatterAggre = new StaticLabelsFormatter(aggregateGraph);
            aggregateGraph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatterAggre);

            aggregateGraph.getViewport().setXAxisBoundsManual(true);
            aggregateGraph.getViewport().setMinX(0);
            aggregateGraph.getViewport().setMaxX(6);

            aggregateGraph.getViewport().setScrollable(true); // enables horizontal scrolling
            aggregateGraph.getViewport().setScalable(true);   //enables scaling
            aggregateGraph.getViewport().setScrollable(true); // enables horizontal scrolling

            // styling
            aggregateDP.setColor(Color.rgb(102, 102, 102));
            aggregateDP.setSpacing(10);

            // draw values on top
            aggregateDP.setDrawValuesOnTop(true);
        }
    }
}








