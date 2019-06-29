import java.util.ArrayList;

/**
 * Created by Ryan on 2018-10-30.
 * @author Ryan Stevens B00695460
 *
 * Starts another thread with a timer running in the background.
 */
public class BooYah_Session{
    private int mediaID, mediaDuration;
    private boolean paused;
    private boolean stopTimer;
    private long startTime, pauseStart;
    private ArrayList<Rated_Moment> ratings;
    private final int nanoFactor = 1000000000;


    public BooYah_Session(int mediaID, int duration) {
        //Start timer on another thread
        this.mediaID = mediaID;
        this.mediaDuration = duration;
        stopTimer = false;
        startTime = System.nanoTime();
        ratings = new ArrayList<>();

        System.out.println(startTime);
    }

    /**
     * --- Methods related to the timer. ---
     */
    //View will constantly call this to get elapsed duration
    public int getSecondsElapsed() {
        return (int)((System.nanoTime() - startTime)/nanoFactor);
    }

    public void resumeTimer() {
        stopTimer = false;
        System.out.println("Resuming");
        //Adjust start time to pretend the pause never happened.
        startTime = startTime + (System.nanoTime() - pauseStart);
    }

    public void pauseTimer() {
        System.out.println("Pausing");
        stopTimer = true;
        pauseStart = System.nanoTime();
    }

    /**
     * --- Methods related to accepting rating inputs. ---
     *
     * @param r Integer representation of the rating.
     */
    public void addRating(int r) {
        ratings.add(new Rated_Moment(getSecondsElapsed(), r));
    }

    public void finishRating() {
        pauseTimer();
        for (int i = 0; i < ratings.size(); i++) {
            System.out.println("Second:\t" + ratings.get(i).getTimeIndex() + "\t\tRating:\t" + ratings.get(i).getRatingString());
        }
    }

    public void fastForward(int numSeconds) {
        startTime += (numSeconds * nanoFactor);
    }

    public void reWind(int numSeconds) {
        fastForward(-numSeconds);
        //If would go below 0 seconds, go to 0 instead.
        if (startTime < System.nanoTime()) {
            startTime = System.nanoTime();
        }
    }

    /**
     * Sets timer to have started X seconds before the present.
     * @param seconds time the counter is set to.
     */
    public void setTime(int seconds) {
        startTime = (System.nanoTime() - (seconds * nanoFactor));
    }
}
