package com.example.jigar.booyahproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jigar.booyahproject.rest.BooyahRestClient;
import com.example.jigar.booyahproject.rest.BooyahRestService;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.response.MediaResponse;
import com.example.jigar.booyahproject.rest.model.response.RatingsResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InfoActivity extends AppCompatActivity {

    private final BooyahRestService apiService = BooyahRestClient.getRetrofitInstance().create(BooyahRestService.class);

    private FloatingActionButton fabSample;
    private Button btnRate, btnReports;
    private TextView txtMediaName, txtMediaType, txtMediaDuration, txtMediaAuthor;
    private ImageView imPoster;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Toolbar navToolbar = findViewById(R.id.headerNav);
        setSupportActionBar(navToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.show();

        navToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent intent = getIntent();
        Bundle container = intent.getExtras();

        final MediaResponse media = (MediaResponse)container.getSerializable(Constants.MEDIA_ITEM);
        final AuthTokenResponse auth =(AuthTokenResponse)container.getSerializable(Constants.AUTH_TOKEN);

        fabSample = findViewById(R.id.fabSample);
        btnRate = findViewById(R.id.btnRate);
        btnReports = findViewById(R.id.btnReports);
        txtMediaName = findViewById(R.id.txtMediaName);
        txtMediaType = findViewById(R.id.txtMediaType);
        txtMediaDuration = findViewById(R.id.txtMediaDuration); //TODO: Transform seconds to 00:00:00 format (ex. use DateUtils.formatElapsedTime(currSeconds);
        txtMediaAuthor = findViewById(R.id.txtMediaAuthor);
        imPoster=findViewById(R.id.imPoster);

        txtMediaName.setText(media.getName());
        txtMediaType.setText(media.getMtype());
        txtMediaAuthor.setText(media.getAuthor());
        txtMediaDuration.setText(String.valueOf(Math.round(media.getDuration())));


        imPoster.setImageResource(R.drawable.mediaimage);

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent reportintent = new Intent(InfoActivity.this , ReportsActivity.class);
                Bundle reportbundle= new Bundle();
                reportbundle.putSerializable(Constants.AUTH_TOKEN,auth);
                reportbundle.putSerializable(Constants.MEDIA_ITEM,media);
                reportintent.putExtras(reportbundle);
                startActivity(reportintent);
            }
        });

        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<RatingsResponse> calllikes = apiService.getMediaUserLikes(auth.getToken(), media.getId(), auth.getId());
                calllikes.enqueue(new Callback<RatingsResponse>() {
                    @Override
                    public void onResponse(Call<RatingsResponse> call, Response<RatingsResponse> response) {
                        final RatingsResponse likes = response.body();

                        Call<RatingsResponse> calldislikes = apiService.getMediaUserDislikes(auth.getToken(),media.getId(),auth.getId());
                        calldislikes.enqueue(new Callback<RatingsResponse>() {
                            @Override
                            public void onResponse(Call<RatingsResponse> call, Response<RatingsResponse> response) {
                                RatingsResponse dislikes = response.body();
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(Constants.AUTH_TOKEN, auth);
                                bundle.putSerializable(Constants.MEDIA_ITEM, media);
                                bundle.putSerializable(Constants.LIKES, likes);
                                bundle.putSerializable(Constants.DISLIKES, dislikes);
                                Intent ratingsintent = new Intent(getApplicationContext(), RatingSessionActivity.class);
                                ratingsintent.putExtras(bundle);
                                startActivity(ratingsintent);
                            }

                            @Override
                            public void onFailure(Call<RatingsResponse> call, Throwable t) {
                                Log.e(Constants.LOG_TAG, t.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<RatingsResponse> call, Throwable t) {
                        Log.e(Constants.LOG_TAG, t.getMessage());
                    }
                });
            }
        });

        fabSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(InfoActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });
    }
}