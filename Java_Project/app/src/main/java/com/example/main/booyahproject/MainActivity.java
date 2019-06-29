package com.example.jigar.booyahproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.example.jigar.booyahproject.rest.BooyahRestClient;
import com.example.jigar.booyahproject.rest.BooyahRestService;
import com.example.jigar.booyahproject.rest.model.response.MediaResponse;
import com.example.jigar.booyahproject.service.RecognitionService;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.request.AuthTokenRequest;
import com.example.jigar.booyahproject.service.RecognitionUpdateReceiver;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TOKEN_FILENAME = "token.json";
    private static final String SAMPLED_AUDIO_FILENAME = "sampled_audio.3gp";

    private final BooyahRestService apiService = BooyahRestClient.getRetrofitInstance().create(BooyahRestService.class);

    private AuthTokenResponse authToken;
    private List<MediaResponse> mediaList;

    private boolean samplingAudio = false;
    private boolean mediaListExists = false;
    private Handler samplingHandler;
    private MediaRecorder mSampler;
    private boolean permissionToRecordAccepted = false;
    private RecognitionUpdateReceiver recogUpdateReceiver;
    private Runnable recogReqRunner;
    private String sampledAudioPath;
    private String[] permissionsArray = {Manifest.permission.RECORD_AUDIO};

    private ImageButton btnSample;

    /**
     * Request permissions callback.
     *
     * @param requestCode
     * @param permissionsArray
     * @param grantResultsArray
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissionsArray, @NonNull int[] grantResultsArray){
        super.onRequestPermissionsResult(requestCode, permissionsArray, grantResultsArray);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResultsArray[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish(); //Finishes app
    }

    /**
     * Start sampling audio.
     */
    private void startSampling(){
        // TODO: fix audio sampling params
        mSampler = new MediaRecorder();
        mSampler.setAudioSource(MediaRecorder.AudioSource.MIC);
        mSampler.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mSampler.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mSampler.setOutputFile(sampledAudioPath);
        try{
            mSampler.prepare();
        } catch (IOException e){
            Log.e(Constants.LOG_TAG, "Sampler prepare() failed");
        }
        mSampler.start();
        samplingHandler.postDelayed(recogReqRunner, 10000);
    }

    /**
     * Stop sampling audio.
     */
    private void stopSampling(){
        mSampler.stop();
        mSampler.reset();
        mSampler.release();
        mSampler = null; //gc
    }

    /**
     * Register new users with api & store authentication
     * info locally. If existing user, pull info.
     */
    private void getAuthToken(){
        final String tokenPath = getExternalCacheDir().getAbsolutePath() + "/" + TOKEN_FILENAME;
        if (!new File(tokenPath).exists()){ //create a signature file
            Call<AuthTokenResponse> call = apiService.getAuthToken(new AuthTokenRequest(UUID.randomUUID().toString()));
            call.enqueue(new Callback<AuthTokenResponse>(){
                @Override
                public void onResponse(Call<AuthTokenResponse> call, Response<AuthTokenResponse> response){
                    authToken = response.body();
                    try{
                        FileWriter writer = new FileWriter(tokenPath);
                        writer.write(new Gson().toJson(authToken));
                        writer.close();
                    } catch (Exception err){
                        Log.e(Constants.LOG_TAG, "Writing auth token failed.");
                        Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<AuthTokenResponse> call, Throwable t){
                    Log.e(Constants.LOG_TAG, t.getMessage());
                    Log.e(Constants.LOG_TAG, "Auth token request failed.");
                    Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                }
            });
        } else{ // read signature file
            try {
                authToken = new Gson().fromJson(new FileReader(tokenPath), AuthTokenResponse.class);
                Log.e(Constants.LOG_TAG, String.format("User id: %d", authToken.getId()));
            } catch (Exception err){
                Log.e(Constants.LOG_TAG, "Reading auth token failed.");
                Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("RestrictedApi") //see: https://stackoverflow.com/a/44926919
    private void setMediaList(final SearchView.SearchAutoComplete searchItem){
        Call<List<MediaResponse>> call = apiService.getMediaList(authToken.getToken());
        call.enqueue(new Callback<List<MediaResponse>>() {
            @Override
            public void onResponse(Call<List<MediaResponse>> call, Response<List<MediaResponse>> response) {
                mediaList = response.body();
                if (mediaList.size() > 0){
                    mediaListExists = true;

                    // set search item
                    final String mediaNames[] = new String[mediaList.size()];
                    for (int i = 0; i < mediaNames.length; i++){
                        MediaResponse media = mediaList.get(i);
                        mediaNames[i] = media.getName();
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, mediaNames);
                    searchItem.setAdapter(adapter);
                    searchItem.setThreshold(1); // suppressed lint errors
                    searchItem.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String query = (String) parent.getItemAtPosition(position);
                            int position2 = Arrays.asList(mediaNames).indexOf(query);
                            MediaResponse media= mediaList.get(position2);
                            Intent infointent = new Intent(MainActivity.this , InfoActivity.class);
                            Bundle infobundle= new Bundle();
                            infobundle.putSerializable(Constants.AUTH_TOKEN,authToken);
                            infobundle.putSerializable(Constants.MEDIA_ITEM,media);
                            infointent.putExtras(infobundle);
                            startActivity(infointent);
                        }
                    });

                    // initialize broadcast receivers
                    recogUpdateReceiver = new RecognitionUpdateReceiver(MainActivity.this, mediaList);
                    LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(recogUpdateReceiver, new IntentFilter(Constants.RECOGNITION_UPDATE));
                }
            }

            @Override
            public void onFailure(Call<List<MediaResponse>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error retrieving media list", Toast.LENGTH_LONG).show();
                Log.e(Constants.LOG_TAG, t.getMessage());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.header_nav, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        // See: https://www.dev2qa.com/android-actionbar-searchview-autocomplete-example/
        // NOTE: src_txt need not be defined. It exists in AppCompat.
        final SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setBackgroundColor(Color.WHITE);
        searchAutoComplete.setTextColor(Color.BLACK);
        setMediaList(searchAutoComplete);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_add_media:
                Intent intentAdd = new Intent(MainActivity.this, AddMediaActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(Constants.AUTH_TOKEN, authToken);
                intentAdd.putExtras(bundle);
                startActivity(intentAdd);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //TODO: figure out how this works & what happens when permission isn't granted
        ActivityCompat.requestPermissions(this, permissionsArray, REQUEST_RECORD_AUDIO_PERMISSION); // request permissions

        getAuthToken();

        Toolbar navToolbar = findViewById(R.id.headerNav);
        setSupportActionBar(navToolbar);
        ActionBar ab = getSupportActionBar();
        ab.show();

        sampledAudioPath = getExternalCacheDir().getAbsolutePath() + "/" + SAMPLED_AUDIO_FILENAME;
        samplingHandler = new Handler();
        recogReqRunner = new Runnable() {
            @Override
            public void run() {
                if (samplingAudio && mediaListExists == true){ // sampling is in progress
                    stopSampling();
                    samplingAudio = !samplingAudio;
                    btnSample.setImageResource(R.drawable.ic_main_cc_sampling_plus);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(Constants.AUTH_TOKEN, authToken);
                    bundle.putString(Constants.SAMPLE_FILENAME, SAMPLED_AUDIO_FILENAME);
                    Intent intent = new Intent(MainActivity.this, RecognitionService.class);
                    intent.putExtras(bundle);
                    startService(intent);
                } else{
                    Toast.makeText(MainActivity.this, "Unable to recognize media", Toast.LENGTH_LONG).show();
                }
            }
        };

        btnSample = findViewById(R.id.btnSample);
        btnSample.setEnabled(true);
        btnSample.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 String txtmsg;
                 if (!samplingAudio) { //resample
                     startSampling();
                     txtmsg = "Sampling started";
                     btnSample.setImageResource(R.drawable.ic_main_cc_sampling);
                 } else { //stop
                     stopSampling();
                     samplingHandler.removeCallbacks(recogReqRunner); //stop recogReqHandler
                     txtmsg = "Sampling stopped";
                     btnSample.setImageResource(R.drawable.ic_main_cc_sampling_plus);
                 }
                 samplingAudio = !samplingAudio;
                 Toast.makeText(MainActivity.this, txtmsg, Toast.LENGTH_LONG).show();
             }
         });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mediaListExists == true) {
            LocalBroadcastManager.getInstance(this).registerReceiver(recogUpdateReceiver, new IntentFilter(Constants.RECOGNITION_UPDATE));
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (mediaListExists == true) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(recogUpdateReceiver);
        }
    }
}
