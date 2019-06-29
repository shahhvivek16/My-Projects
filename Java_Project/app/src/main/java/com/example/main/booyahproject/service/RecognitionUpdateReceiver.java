package com.example.jigar.booyahproject.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import com.example.jigar.booyahproject.Constants;
import com.example.jigar.booyahproject.MainActivity;
import com.example.jigar.booyahproject.RatingSessionActivity;
import com.example.jigar.booyahproject.rest.BooyahRestClient;
import com.example.jigar.booyahproject.rest.BooyahRestService;
import com.example.jigar.booyahproject.rest.model.response.MediaResponse;
import com.example.jigar.booyahproject.rest.model.response.RecognitionResponse;

import java.util.List;

public class RecognitionUpdateReceiver extends BroadcastReceiver {

    private final BooyahRestService apiService = BooyahRestClient.getRetrofitInstance().create(BooyahRestService.class);
    private MainActivity mainctx;
    private List<MediaResponse> mediaList;

    public RecognitionUpdateReceiver(){}

    public RecognitionUpdateReceiver(MainActivity mainctx, List<MediaResponse> mediaList){
        this.mainctx = mainctx;
        this.mediaList = mediaList;
    }

    @Override
    public void onReceive(Context ctx, Intent intent){
        Bundle bundle = intent.getExtras();

        if(bundle != null){
            RecognitionResponse response = (RecognitionResponse) bundle.getSerializable(Constants.RECOG_OBJ);

            //TODO: Vibrate once there is a match
            Vibrator v = (Vibrator) this.mainctx.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));

            String msg = "Recognized " + response.getName() + " @ " + String.valueOf(Math.round(response.getOffset()) / 60) + ":" + String.valueOf(Math.round(response.getOffset()) % 60);
            Toast.makeText(this.mainctx, msg, Toast.LENGTH_LONG).show();

            // get MediaResponse that was stored locally
            MediaResponse recognizedMedia = null;
            for(int i=0; i < this.mediaList.size(); i++){
                MediaResponse media = this.mediaList.get(i);
                if (media.getId() == response.getId()){
                    recognizedMedia = media;
                }
            }

            Intent ratingsIntent = new Intent(this.mainctx, RatingSessionActivity.class);
            bundle.putSerializable(Constants.MEDIA_ITEM, recognizedMedia);
            bundle.putSerializable(Constants.TIME_OFFSET, Math.round(response.getOffset()));
            ratingsIntent.putExtras(bundle);
            this.mainctx.startActivity(ratingsIntent);
        }
    }
}
