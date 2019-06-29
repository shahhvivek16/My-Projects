package com.example.jigar.booyahproject;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.example.jigar.booyahproject.rest.model.request.RatingsRequest;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.response.RatingsResponse;
import com.example.jigar.booyahproject.rest.model.response.MediaResponse;
import com.example.jigar.booyahproject.service.RatingSession;

/**
 * When switching to this view, assume we have all the data needed to star the rating session right away.
 *
 */
public class RatingSessionActivity extends AppCompatActivity {

    private static RatingSession session;
    private TextView txtTimeElapsed, txtMediaName;
    private Button btnDone;
    private ImageButton btnBoo, btnYah, btnPause, btnFF, btnRR;
    private static final String boo = "Boo!";
    private static final String yah = "Yah!";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rating_screen);

        int startTimeOffset;
        RatingsResponse goodSecsPassed, badSecsPassed;
        RatingsRequest goodSecs, badSecs;
        AuthTokenResponse authToken;
        MediaResponse media;

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        Toolbar navToolbar = findViewById(R.id.headerNav);
        setSupportActionBar(navToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.show();

        final Animation animScale = AnimationUtils.loadAnimation(this, R.anim.anim_scale);

        // see: https://stackoverflow.com/a/47975023
        // fixes bug where navToolbar creates new parent activity
        /* Fixes bug where navToolbar creates new parent activity
         * See: https://stackoverflow.com/a/47975023
         */
        navToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //Get data from the intent's bundle.
        authToken = (AuthTokenResponse) bundle.getSerializable(Constants.AUTH_TOKEN);
        media = (MediaResponse) bundle.getSerializable(Constants.MEDIA_ITEM);
        startTimeOffset = bundle.getInt(Constants.TIME_OFFSET, 0); //Will be 0 if none was passed in.
        goodSecsPassed = (RatingsResponse) bundle.getSerializable(Constants.LIKES);
        badSecsPassed = (RatingsResponse) bundle.getSerializable(Constants.DISLIKES);

        //Error handling for bad data being passed in.
        if (media == null || authToken == null || media.getId() == 0 || media.getDuration() == 0) {
            Toast.makeText(getApplicationContext(), Constants.TOAST_ERROR, Toast.LENGTH_SHORT).show();
            Log.e("Starting page", "A piece of the media object was not set.");
            finish();
        }

        /* Create the ratingRequests that will have seconds added to them.
         * Different constructor based on whether or not there is existing data from a previous session.
         */
        if (badSecsPassed == null || badSecsPassed.seconds == null){
            badSecs = new RatingsRequest();
        } else {
            badSecs = new RatingsRequest(badSecsPassed.seconds);
        }

        if (goodSecsPassed == null || badSecsPassed.seconds == null) {
            goodSecs = new RatingsRequest();
        } else {
            goodSecs = new RatingsRequest(goodSecsPassed.seconds);
        }

        txtMediaName = findViewById(R.id.mediaTitle);
        txtTimeElapsed = findViewById(R.id.timerDisplay);
        btnPause = findViewById(R.id.btnPlay);
        btnFF = findViewById(R.id.btnForward);
        btnRR = findViewById(R.id.btnRewind);
        btnBoo = findViewById(R.id.btnBad);
        btnYah = findViewById(R.id.btnGood);
        btnDone = findViewById(R.id.btnFinished);

        txtMediaName.setText(media.getName() + " - " + media.getAuthor());

        //START THE SESSION
        session = new RatingSession(authToken, startTimeOffset, goodSecs, badSecs, media.getDuration(), media.getId());

        //Allow UI to be updated. UI refreshes every 0.1 seconds
        RatingSessionViewUpdater uiUpdater = new RatingSessionViewUpdater(session, this);
        //Timer timer = new Timer();
        //timer.schedule(uiUpdater, 100, 100);    //UI refreshes every 0.1 seconds.
        new Timer().schedule(uiUpdater, 100, 100);

        btnFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                session.fastForward(10);
                if (session.atMaxTime()) {
                    btnPause.setEnabled(false);
                }
            }
        });

        /*
         * Move timer 10 seconds forward, and ensure buttons are in correct state.
         */
        btnRR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (session.isPaused()) {  //We know we are paused and will want to continue manually.
                    btnPause.setImageResource(R.drawable.ic_rating_play);
                }

                btnFF.setEnabled(true);
                btnPause.setEnabled(true);
                session.reWind(10);
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (session.isPaused()) {       //If currently paused, play
                    session.resumeTimer();
                    btnPause.setImageResource(R.drawable.ic_rating_pause);
                    btnDone.setEnabled(false);
                    btnDone.setBackgroundColor(ContextCompat.getColor(RatingSessionActivity.this, R.color.colorAshGrey));
                } else {                        //If currently playing, pause
                    session.pauseTimer();
                    btnPause.setImageResource(R.drawable.ic_rating_play);
                    btnDone.setEnabled(true);       //User can now finish the rating session.
                    btnDone.setBackgroundColor(ContextCompat.getColor(RatingSessionActivity.this, R.color.colorYellow));
                }
            }
        });
        //Ensure pause button has correct icon.
        btnPause.setImageResource(R.drawable.ic_rating_pause);

        btnBoo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(animScale);
                btnBoo.setImageResource(R.drawable.ic_rating_down_solid);
                session.addBadRating();
                Toast.makeText(RatingSessionActivity.this, boo, Toast.LENGTH_SHORT).show();
            }
        });

        btnYah.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                view.startAnimation(animScale);
                btnYah.setImageResource(R.drawable.ic_rating_up_solid);
                session.addGoodRating();
                Toast.makeText(RatingSessionActivity.this, yah , Toast.LENGTH_SHORT).show();
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnDone.setEnabled(false);      //Prevent double-clicking
                btnDone.setBackgroundColor(ContextCompat.getColor(RatingSessionActivity.this, R.color.colorAshGrey));
                session.finishRating();
                finish();
            }
        });
        btnDone.setEnabled(false);      //User can not end the session until they pause, or timer is finished.
        btnDone.setBackgroundColor(ContextCompat.getColor(RatingSessionActivity.this, R.color.colorAshGrey));
    }

    //--    Wrapper methods for the uiUpdater to call on the UI thread.  --//
    /**
     * Updates the timer, and resets the rating button icons.
     *
     * @param updatedTime Formatted time that is to the displayed.
     */
    protected void updateTimerLabel(String updatedTime) {
        txtTimeElapsed.setText(updatedTime);
    }

    protected void setMaxTimeState() {
        btnDone.setEnabled(true);
        btnDone.setBackgroundColor(ContextCompat.getColor(RatingSessionActivity.this, R.color.colorYellow));
        btnPause.setEnabled(false);
        btnPause.setImageResource(R.drawable.ic_rating_play);
        btnFF.setEnabled(false);
    }

    protected void setLikedState() {
        btnYah.setImageResource(R.drawable.ic_rating_up_solid);
        btnBoo.setImageResource(R.drawable.ic_rating_down);
    }

    protected void setDislikedState() {
        btnBoo.setImageResource(R.drawable.ic_rating_down_solid);
        btnYah.setImageResource(R.drawable.ic_rating_up);
    }

    protected void setNeutralState() {
        btnYah.setImageResource(R.drawable.ic_rating_up);
        btnBoo.setImageResource(R.drawable.ic_rating_down);
    }
}

/**
 *  Update the UI every 0.1 seconds.
 *  Not using android.os.countDownTimer because we can't know how long the session will be (i.e. if they pause, RR, or FF).
 *  Calls wrapper methods from the view, using the uiThread, Since other threads can't access the view by themselves
 */
class RatingSessionViewUpdater extends TimerTask{
    private RatingSession session;
    private String formattedTime;
    private RatingSessionActivity view;

    /**
     * @param session   The rating session's controller
     * @param v         View that will be updated.
     */
    public RatingSessionViewUpdater(RatingSession session, RatingSessionActivity v) {
        this.session = session;
        this.view = v;
        Log.d("Time Update process", "Created updater");
    }

    @Override
    public void run() {
        boolean likedOrDislikedState = false;

        //Step 1: Update timer.
        formattedTime = DateUtils.formatElapsedTime(session.getSecondsElapsed());
        view.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.updateTimerLabel(formattedTime);
            }
        });

        //Step 2: If at max time state, and not yet paused, pause.
        if (!session.isPaused() && session.atMaxTime()) {
            session.pauseTimer();
            view.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setMaxTimeState();
                }
            });
        }

        //Step 3: Update UI to show that this moment is "liked"
        if (session.currMomentGood()) {
            likedOrDislikedState = true;
            view.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setLikedState();
                }
            });
        }

        if (session.currMomentBad()) {
            likedOrDislikedState = true;
            view.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setDislikedState();
                }
            });
        }

        if (!likedOrDislikedState){     //Only if neither of the above states have been set.
            view.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setNeutralState();
                }
            });
        }
    }
}