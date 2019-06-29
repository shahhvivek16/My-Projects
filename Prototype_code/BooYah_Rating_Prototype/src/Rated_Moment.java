/**
 * Created by Ryan on 2018-10-30.
 * Simple object to hold ratings for a given moment.
 */
public class Rated_Moment {
    private int timeIndex, rating;

    /**
     * @param timeIndex In (regular) seconds
     * @param rating Int representation of the rating for that moment
     * 1 = Boo! (worst)
     * 2 = Bad
     * 3 = Natural (may not use this one)
     * 4 = Good
     * 5 = Yah! (best)
     */
    public Rated_Moment(int timeIndex, int rating) {
        this.timeIndex = timeIndex;
        this.rating = rating;
    }

   @Override
    public String toString() {
        return (timeIndex + ":" + rating);
    }

    public int getTimeIndex() {
        return timeIndex;
    }

    public int getRating() {
        return rating;
    }

    public String getRatingString() {
        String english;

        switch (this.rating){
            case 1:
                english = "Boo!";
                break;
            case 2:
                english = "Bad";
                break;
            case 3:
                english = "Netural";
                break;
            case 4:
                english = "Good";
                break;
            case 5:
                english = "Yah!";
                break;
            default:
                english = "Error";
        }
        return english;
    }
}
