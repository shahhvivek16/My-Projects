package com.example.jigar.booyahproject.service;

import android.app.IntentService;
import android.content.Intent;
import android.media.Rating;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.jigar.booyahproject.Constants;
import com.example.jigar.booyahproject.MainActivity;
import com.example.jigar.booyahproject.rest.BooyahRestClient;
import com.example.jigar.booyahproject.rest.BooyahRestService;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.response.RatingsResponse;
import com.example.jigar.booyahproject.rest.model.response.RecognitionResponse;
import com.example.jigar.booyahproject.rest.model.response.StatusResponse;

import java.io.File;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * RecognitionService
 *
 * see: https://stackoverflow.com/a/6343299 & https://stackoverflow.com/a/21284021
 * see: http://www.vogella.com/tutorials/AndroidServices/article.html#service_intentservices
 */
public class RecognitionService extends IntentService {

    private final BooyahRestService apiService = BooyahRestClient.getRetrofitInstance().create(BooyahRestService.class);

    public RecognitionService(){
        super("RecognitionService");
    }

    private void publish(RecognitionResponse response, RatingsResponse likes, RatingsResponse dislikes){
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.RECOG_OBJ, response);
        bundle.putSerializable(Constants.LIKES, likes);
        bundle.putSerializable(Constants.DISLIKES, dislikes);
        Intent intent = new Intent(Constants.RECOGNITION_UPDATE);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        Bundle bundle = intent.getExtras();
        final AuthTokenResponse userToken = (AuthTokenResponse) bundle.getSerializable(Constants.AUTH_TOKEN);
        String audioFilename = bundle.getString(Constants.SAMPLE_FILENAME);

        Log.i(Constants.LOG_TAG, "Executing Recognition service: " + intent);
        // see: https://stackoverflow.com/a/36514662
        String filepath = getExternalCacheDir().getAbsolutePath() + "/" + audioFilename;
        MultipartBody.Part formData = MultipartBody.Part.createFormData(
                "file",
                audioFilename,
                RequestBody.create(MediaType.parse("multipart/form-data"), new File(filepath))
        );

        Call<StatusResponse> call0 = apiService.recognizeMedia(userToken.getToken(), formData);
        try {
            StatusResponse status = call0.execute().body(); // synchronous call since we're on different thread
            Call<Object> call1 = apiService.getRecognizeMediaStatus(userToken.getToken(), status.getUuid());
            Object result = call1.execute().body();
            int retry = 3;
            while (retry > 0 && result.toString().contains("uuid")){
                Thread.sleep(200);
                call1 = apiService.getRecognizeMediaStatus(userToken.getToken(), status.getUuid());
                result = call1.execute().body();
                retry--;
            }

            if (result.toString().contains("name")){
                final RecognitionResponse recogres = RecognitionResponse.createFromObject(result);
                Call<RatingsResponse> likesCall = apiService.getMediaUserLikes(userToken.getToken(), recogres.getId(), userToken.getId());
                final Call<RatingsResponse> dislikesCall = apiService.getMediaUserDislikes(userToken.getToken(), recogres.getId(), userToken.getId());

                //TODO: Look into making parallel calls i.e RxJava
                likesCall.enqueue(new Callback<RatingsResponse>() {
                    @Override
                    public void onResponse(Call<RatingsResponse> call, Response<RatingsResponse> response) {
                        final RatingsResponse ulikes = response.body();

                        dislikesCall.enqueue(new Callback<RatingsResponse>() { //get dislikes
                            @Override
                            public void onResponse(Call<RatingsResponse> call, Response<RatingsResponse> response) {
                                // init empty ratings if likes or/and dislikes is empty
                                final RatingsResponse dislikes = (response.body().seconds == null ? new RatingsResponse(userToken.getId(), recogres.getId()): response.body());
                                final RatingsResponse likes = (ulikes.seconds == null ? new RatingsResponse(userToken.getId(), recogres.getId()) : ulikes);
                                publish(recogres, likes, dislikes);
                            }
                            @Override
                            public void onFailure(Call<RatingsResponse> call, Throwable t) {
                                Log.e(Constants.LOG_TAG, t.getMessage());
                                Log.e(Constants.LOG_TAG, "Failed to retrieve dislikes.");
                            }
                        });
                    }
                    @Override
                    public void onFailure(Call<RatingsResponse> call, Throwable t) {
                        Log.e(Constants.LOG_TAG, t.getMessage());
                        Log.e(Constants.LOG_TAG,"Failed to retrieve likes.");
                    }
                });
            }
        } catch (Exception err) {
            Log.e(Constants.LOG_TAG, "Error retrieving recognition status.");
            Log.e(Constants.LOG_TAG, err.toString());
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
