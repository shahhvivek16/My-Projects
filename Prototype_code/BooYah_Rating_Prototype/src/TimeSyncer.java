/**
 * Created by Ryan on 2018-11-16.
 * Instigated thread that listens to audio and attempts to adjust the rating session as needed.
 * If canceled, thread is allowed to die and new one will be created later, if needed.
 */
public class TimeSyncer extends Thread{
    int mediaID;
    BooYah_Session session;
    boolean stopTimer;

    /**
     * Called by the View when user presses tick box.
     *
     * @param s Session that will have its timer updated.
     * @param mID Media that may be indexed in database.
     */
    public TimeSyncer(BooYah_Session s, int mID) {
        session = s;
        mediaID = mID;
        stopTimer = false;

        //Do initial check to see if content is in DB.

        this.start();
    }

    public void cancelSync() {
        stopTimer = true;
    }

    /**
     * Poll the database to try and
     */
    @Override
    public void run() {
        do{
            try{
                sleep(5000);
                //sleep(1000);



            }
            catch (InterruptedException e){
                e.printStackTrace();
            }

            //Print time index.
            //System.out.println(session.getSecondsElapsed());
        } while(!stopTimer);
    }


}
