/**
 * Created by Shivani Desai on 15th Nov 2018
 * This class enables users to add media and store it in the database
 * last edited on 5th Dec 2018
 */


package com.example.jigar.booyahproject;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.example.jigar.booyahproject.rest.BooyahRestClient;
import com.example.jigar.booyahproject.rest.BooyahRestService;
import com.example.jigar.booyahproject.rest.model.request.CreateMediaRequest;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.response.StatusResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AddMediaActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    //API Service Instance
    private final BooyahRestService apiService = BooyahRestClient.getRetrofitInstance().create(BooyahRestService.class);

    private FloatingActionButton fabSample;
    private EditText etMediaName, etMediaUrl, etMediaArtist;
    private Button btnAddMedia;
    private Spinner etMediaType;
    private TimePicker tpDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_media);


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
        Bundle bundle = intent.getExtras();

        //get Authentication token
        final AuthTokenResponse userToken = (AuthTokenResponse) bundle.getSerializable(Constants.AUTH_TOKEN);

        fabSample = findViewById(R.id.fabSample);
        etMediaName = findViewById(R.id.etMediaName);
        etMediaArtist = findViewById(R.id.etMediaArtist);
        etMediaUrl = findViewById(R.id.etMediaUrl);
        etMediaType = findViewById(R.id.etMediaType);
        btnAddMedia = findViewById(R.id.btnAddMedia);
        tpDuration =  findViewById(R.id.tpDuration);
        tpDuration.setIs24HourView(true);

        //Spinner click listener
        etMediaType.setOnItemSelectedListener(this);

        //Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("Music");
        categories.add("Movie");
        categories.add("Television");

        // Creating adapter for spinner
        final ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        etMediaType.setAdapter(dataAdapter);

        // Button click listener
        btnAddMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Get user input
                final String mediaName = etMediaName.getText().toString();
                final String mediaArtist = etMediaArtist.getText().toString();
                final int hour = tpDuration.getCurrentHour();
                final int min = tpDuration.getCurrentMinute();

                //convert media duration from hours, minutes to seconds
                final Integer mediaDuration = (60 * min) + (3600 * hour);
                final String mediaUrl = etMediaUrl.getText().toString();
                final String mediaType="";

                //Create media request
                CreateMediaRequest newMedia;

                //Check if media URL is added by user
                if(mediaUrl.isEmpty()) {
                    newMedia = new CreateMediaRequest(mediaName, mediaDuration, mediaArtist, mediaType);
                } else{
                    newMedia = new CreateMediaRequest(mediaName, mediaDuration, mediaArtist, mediaType, mediaUrl);
                }

                //Call API
                Call<StatusResponse> media = apiService.createNewMedia(userToken.getToken(), newMedia);

                //Send request asynchronously
                media.enqueue(new Callback<StatusResponse>() {
                    @Override
                    //Callback method invoked for HTTP request
                    public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                        Toast.makeText(getApplicationContext(), "Media added", Toast.LENGTH_LONG);
                        etMediaName.setText("");
                        etMediaArtist.setText("");
                      //  etMediaDuration.setText("");
                        etMediaUrl.setText("");
                    }

                    @Override
                    //Callback method invoked when connection fails
                    public void onFailure(Call<StatusResponse> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Media cannot be added", Toast.LENGTH_LONG);
                    }
                });
            }

        });

        //Floating Action Bar
        fabSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(AddMediaActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // On selecting a spinner item
        String mediaType = adapterView.getItemAtPosition(i).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        //No spinner item selected
        String mediaType = adapterView.getItemAtPosition(0).toString();
    }
}
