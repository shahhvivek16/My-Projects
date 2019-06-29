package com.example.jigar.booyahproject.service;

import android.util.Log;

import com.example.jigar.booyahproject.rest.BooyahRestClient;
import com.example.jigar.booyahproject.rest.BooyahRestService;
import com.example.jigar.booyahproject.rest.model.request.RatingsRequest;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.response.RatingsResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Ryan on 2018-10-30.
 * @author Ryan Stevens B00695460
 *
 */
public class RatingSession {
    private int mediaDuration, mediaID;
    private boolean paused;
    private long startTime, pauseStart;
    private final int MICROFACTOR = 1000;
    private boolean newGoodRating, newBadRating;
    private RatingsRequest goodSeconds, badSeconds;
    private AuthTokenResponse authToken;
    private final BooyahRestService apiService = BooyahRestClient.getRetrofitInstance().create(BooyahRestService.class);

    /**
     *
     * @param startOffset   Time the timer stats at.
     * @param goodSeconds   Object that holds the good seconds.
     * @param badSeconds    Object that holds the bad seconds.
     * @param duration      Length of the subject media, in seconds.
     */
    public RatingSession(AuthTokenResponse authToken, int startOffset, RatingsRequest goodSeconds, RatingsRequest badSeconds, int duration, int mediaID) {
        paused = false;
        this.goodSeconds = goodSeconds;
        this.badSeconds = badSeconds;
        this.mediaDuration = duration;
        this.authToken = authToken;
        this.mediaID = mediaID;

        newGoodRating = this.goodSeconds.seconds.isEmpty();
        newBadRating = this.badSeconds.seconds.isEmpty();

        //"Start" the timer.
        this.startTime = System.currentTimeMillis() - (startOffset * MICROFACTOR);
    }

    /**
     * --- Methods related to the timer. ---
     */
    //View will constantly call this to get elapsed duration
    public int getSecondsElapsed() {
        if (paused) {
            return (int)((pauseStart - startTime)/ MICROFACTOR);
        } else {
            return (int)((System.currentTimeMillis() - startTime)/ MICROFACTOR);
        }
    }

    /**
     * Adjust start time to pretend the pause never happened.
     */
    public void resumeTimer() {
        if (paused) {
            paused = false;
            startTime = startTime + (System.currentTimeMillis() - pauseStart);
        }
    }

    /**
     * If not already paused, pause the timer.
     */
    public void pauseTimer() {
        if (!paused) {
            paused = true;
            pauseStart = System.currentTimeMillis();
        }
    }

    /**
     * Called on every UI update tick, as long as not paused already.
     * @return  Whether or not the timer is beyond the max time.
     */
    public boolean atMaxTime() {
        if (getSecondsElapsed() >= mediaDuration) {
            setTime(mediaDuration);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Called by pressing buttons on the UI.
     * Does not add if the second if already in the List.
     * Also attempts to removes moment (current +/- 1 second) from the other list
     * Both goodRating and badRating version are the same, just using different lists.
     */
    public void addGoodRating() {
        Integer currTime = getSecondsElapsed();

        if (!goodSeconds.seconds.contains(currTime)) {
            goodSeconds.seconds.add(currTime);
            Log.d("Add Rating", "Good second at: " + getSecondsElapsed());
        }
        badSeconds.seconds.remove(currTime);
        currTime += 1;      //Need to update object, else parameter is the index.
        badSeconds.seconds.remove(currTime);
        currTime -= 2;
        badSeconds.seconds.remove(currTime);
    }

    public void addBadRating() {
        Integer currTime = getSecondsElapsed();

        if (!badSeconds.seconds.contains(currTime)) {
            badSeconds.seconds.add(currTime);
            Log.d("Add Rating", "Bad second at: " + getSecondsElapsed());
        }
        goodSeconds.seconds.remove(currTime);
        currTime += 1;
        goodSeconds.seconds.remove(currTime);
        currTime -= 2;
        goodSeconds.seconds.remove(currTime);
    }

    /**
     * Make the API PUSH/PUT request to save the rating data.
     * By the time this is called, timer has been paused/stopped by the View.
     * Will only send the data if there is at least one entry for that each.
     * View does not wait for the 2 callbacks before it returns to the Media info page.
     */
    public void finishRating() {
        Call<RatingsResponse> yahRequest, booRequest;

        if (!goodSeconds.seconds.isEmpty()) {
            if (newGoodRating){
                yahRequest = apiService.createMediaUserLikes(authToken.getToken(), mediaID, authToken.getId(), goodSeconds);
            } else {
                yahRequest = apiService.updateMediaUserLikes(authToken.getToken(), mediaID, authToken.getId(), goodSeconds);
            }

            Log.d("SUBMISSION CONTENT", "Good Seconds:\t" + goodSeconds.seconds.toString());

            yahRequest.enqueue(new Callback<RatingsResponse>() {
                @Override
                public void onResponse(Call<RatingsResponse> call, Response<RatingsResponse> response) {
                    Log.d("SUBMISSION RESULT", "Yah moment data successfully sent.");
                }

                @Override
                public void onFailure(Call<RatingsResponse> call, Throwable t) {
                    Log.e("SUBMISSION RESULT", "Failed to send Yah moment data.");
                }
            });
        }

        if (!badSeconds.seconds.isEmpty()){
            if (newBadRating){
                booRequest = apiService.createMediaUserDislikes(authToken.getToken(), mediaID, authToken.getId(), badSeconds);
            } else {
                booRequest = apiService.updateMediaUserDislikes(authToken.getToken(), mediaID, authToken.getId(), badSeconds);
            }

            Log.d("SUBMISSION CONTENT", "Bad Seconds:\t" + badSeconds.seconds.toString());

            booRequest.enqueue(new Callback<RatingsResponse>() {
                @Override
                public void onResponse(Call<RatingsResponse> call, Response<RatingsResponse> response) {
                    Log.d("SUBMISSION RESULT", "Boo moment data successfully sent.");
                }

                @Override
                public void onFailure(Call<RatingsResponse> call, Throwable t) {
                    Log.e("SUBMISSION RESULT", "Failed to send Boo moment data.");
                }
            });
        }
    }

    /**
     * Used to update the UI with an indication that the moment is good/bad.
     * Checks if the current second, +/- 1 second, is liked/disliked.
     */
    public boolean currMomentGood() {
        int currSecond = getSecondsElapsed();
        return (goodSeconds.seconds.contains(currSecond) || goodSeconds.seconds.contains(currSecond - 1) || goodSeconds.seconds.contains(currSecond + 1));
    }

    public boolean currMomentBad() {
        int currSecond = getSecondsElapsed();
        return (badSeconds.seconds.contains(currSecond) || badSeconds.seconds.contains(currSecond - 1) || badSeconds.seconds.contains(currSecond + 1));
    }

    /**
     * Set "start time" to be further in the past
     * If would go beyond end time, go to end time instead.
     */
    public void fastForward(int numSeconds) {
        startTime = startTime - (numSeconds * MICROFACTOR);
        if (atMaxTime()) {
            setTime(mediaDuration);
        }
    }

    /**
     * Set "start time" to be further in the future.
     * If would go below 0 seconds, reset to 0 instead.
     */
    public void reWind(int numSeconds) {
        startTime = startTime + (numSeconds * MICROFACTOR);
        if (getSecondsElapsed()  < 0) {
            startTime = System.currentTimeMillis();
        }
    }

    /**
     * Sets timer to have started X seconds before the present.
     * @param seconds time the counter is set to.
     */
    public void setTime(int seconds) {
        if (paused) {
            startTime = (pauseStart - (seconds * MICROFACTOR));
        }  else {
            startTime = (System.currentTimeMillis() - (seconds * MICROFACTOR));
        }
    }

    public boolean isPaused() {
        return paused;
    }
}

