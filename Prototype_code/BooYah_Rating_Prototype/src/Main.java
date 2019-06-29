import java.util.Scanner;

/**
 * Created by Ryan on 2018-10-30.
 *
 * Disposable view prototype, used to develop BooYah_Session control object.
 *
 * Simple way to provide input to the BooYah Session object
 * Modeled after code developed for Ryan Stevens' Assignment 2 for Mobile Computing.
 */
public class Main {
    static BooYah_Session ratingSession;
    static TimerViewUpdater timer;

    public static void main(String args[]) {
        System.out.println("Hello World");
        ratingSession = new BooYah_Session(33, 86400);
        timer = new TimerViewUpdater(ratingSession);

        String userInput = "0";
        Scanner sc = new Scanner(System.in);

        while (!userInput.equals("END")){
            handelInput(userInput);
            userInput = sc.next();
            //System.out.println(ratingSession.getSecondsElapsed());
        }

        System.out.println("Program over. Have a nice day!");
        //ratingSession.pauseTimer();
        ratingSession.finishRating();
        timer.stopTimer = true;
    }

    private static void handelInput(String input) {

        switch(input) {
            case "z":       //Boo!
                ratingSession.addRating(1);
                break;
            case "x":       //Bad
                ratingSession.addRating(2);
                break;
            case "c":       //Good
                ratingSession.addRating(4);
                break;
            case "v":       //Yah!
                ratingSession.addRating(5);
                break;
            case "p":       //pause
                timer.stopTimer = true;
                ratingSession.pauseTimer();
                //Pause
                break;
            case "r":           //Resume
                ratingSession.resumeTimer();
                timer = new TimerViewUpdater(ratingSession);
            case "t":       //Show timer
                System.out.println(ratingSession.getSecondsElapsed());
                break;
            case "b":
                ratingSession.reWind(5);
            default:
                System.out.println("Error - input not recognized.");
        }
    }
}

class TimerViewUpdater extends Thread{
    BooYah_Session session;
    boolean stopTimer = false;
    TimerViewUpdater(BooYah_Session s){
        session = s;
        this.start();
    }

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
            System.out.println(session.getSecondsElapsed());
        } while(!stopTimer);
    }

}
