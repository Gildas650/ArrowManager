package fr.arrowm.arrowm.Business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Class created to managed a Session of training or competition
 * numberOfArrows : Number of arrows targeted during session
 * begin and end of a session
 * isCompetition
 * existRound to know if a round is already created
 * An object Round to count points
 * A comment only in competition case
 * A dbID if the object is retreived from DB
 * Created by Gildas on 13/12/2016.
 */

public class Session implements Serializable {
    private int numberOfArrows;
    private Date beginOfSession;
    private Date endOfSession;
    private boolean existRound;
    private Round round;
    private Integer BLEState;

    private ArrayList<Integer> arrowTime;
    private int dbId = -1;

    public Session() {
        this.beginOfSession = new Date();
        this.existRound = false;
        this.arrowTime = new ArrayList<>();
        this.BLEState = 0;
    }

    public void closeSession() {
        this.endOfSession = new Date();
    }

    public void addArrow() {
        this.numberOfArrows++;
    }

    public void removeArrow() {
        if (this.numberOfArrows > 0) {
            this.numberOfArrows--;
        }
    }

    public int getNumberOfArrows() {
        return numberOfArrows;
    }

    public void setNumberOfArrows(int numberOfArrows) {
        this.numberOfArrows = numberOfArrows;
    }

    public Date getBeginOfSession() {
        return beginOfSession;
    }

    public void setBeginOfSession(Date beginOfSession) {
        this.beginOfSession = beginOfSession;
    }

    public Date getEndOfSession() {
        return endOfSession;
    }

    public void setEndOfSession(Date endOfSession) {
        this.endOfSession = endOfSession;
    }

    public void setRound(Event e) {
        this.round = new Round(e);
        this.existRound = true;
    }

    public boolean isExistRound() {
        return existRound;
    }


    //Define how many time a session take from begin to end if end is already define or to the current time if the session is still running
    public String toStringSessionDuration() {
        String ret;
        String sh;
        String sm;
        String ss;
        Date deb = this.getBeginOfSession();
        Date fin;
        if (this.endOfSession != null) {
            fin = this.endOfSession;
        } else {
            fin = new Date();
        }
        long diff = fin.getTime() - deb.getTime();
        int s = (int) (diff / 1000) % 60;
        int m = (int) ((diff / (1000 * 60)) % 60);
        int h = (int) ((diff / (1000 * 60 * 60)) % 24);

        if (h < 10) {
            sh = "0" + h;
        } else {
            sh = h + "";
        }

        if (m < 10) {
            sm = "0" + m;
        } else {
            sm = m + "";
        }

        if (s < 10) {
            ss = "0" + s;
        } else {
            ss = s + "";
        }
        ret = sh + ":" + sm + ":" + ss;
        return ret;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
        this.existRound = true;
    }

    public ArrayList<Integer> getArrowTime() {
        return arrowTime;
    }

    public void addArrowTime(Integer arrowTime) {
        this.arrowTime.add(arrowTime);
    }

    public Integer getBLEState() {
        return BLEState;
    }

    public void setBLEState(Integer BLEState) {
        this.BLEState = BLEState;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }
}
