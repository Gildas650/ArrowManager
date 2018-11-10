package fr.arrowm.arrowm.Business;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;


/**
 * Class created to store all information related to score management
 * Event : Event of the Round
 * scorecard : list of the round scores
 * impactcard : list of the impact in touchview
 * count : technical variable used split lists into the different target session
 * nbArrowsbyShoot : tachnical variable to get the number of arrows by Shoot
 * Created by Gildas on 13/12/2016.
 */

public class Round implements Serializable {

    private Event event;
    private ArrayList<Score> scorecard;
    private ArrayList<Impact> impactcard;
    private int count = 0;
    private int nbArrowsbyShoot;

    public Round(Event e) {
        this.event = e;
        nbArrowsbyShoot = this.event.getShoot();
        this.scorecard = new ArrayList<>();
        this.impactcard = new ArrayList<>();
    }

    public Event getEvent() {
        return event;
    }

    public ArrayList<Score> getScorecard() {
        return scorecard;
    }

    //Add an arrow with score and impact and sort it into the nb of arrows by shoot with management of X value
    public void addArrowScore(Score s, Impact imp) {

        if (count == nbArrowsbyShoot) {
            count = 0;
        }
        int inc = Math.round(this.scorecard.size() / nbArrowsbyShoot);
        int index = this.scorecard.size();
        for (int i = (inc * nbArrowsbyShoot); i < ((inc * nbArrowsbyShoot) + count); i++) {
            int tmp = this.scorecard.get(i).getValue();
            int tmp2 = s.getValue();

            if (this.scorecard.get(i).getName().equals("X")) {
                tmp = tmp + 1;
            }
            if (s.getName().equals("X")) {
                tmp2 = tmp2 + 1;
            }
            if (tmp < tmp2 && i < index) {
                index = i;
            }
        }
        scorecard.add(index, s);
        impactcard.add(index, imp);
        count++;
    }

    //Remove an arrow in lists
    public void removeArrowScore() {
        if (this.scorecard.size() > 0) {
            this.scorecard.remove(this.scorecard.size() - 1);
            this.impactcard.remove(this.impactcard.size() - 1);
            if (count > 0) {
                count--;
            } else {
                count = nbArrowsbyShoot - 1;
            }
        }
    }

    //Get the number of score card of the round
    public int getNumberOfScoreCard() {
        return (int) Math.ceil((this.scorecard.size() - 1) / (this.event.getShoot() * this.event.getArrowsByShoot()));
    }

    public int getNumberOfArrows() {
        return scorecard.size();
    }

    public float[] getStats() {
        float ret[] = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int nbArrows = 0;
        //[0] Total
        //[1] Moy
        //[2] nb10
        //[3] nb9
        //[4] nb8
        //[5] subTotal Round 1
        //[6] subTotal Round 2
        //[7] subTotal Round 3
        //[8] nb volée

        for (int i = 0; i < this.scorecard.size(); i++) {
            int nbArrowsbyShot = this.event.getShoot() * this.event.getArrowsByShoot();
            Score temp = this.scorecard.get(i);
            int value = temp.getValue();
            ret[0] = ret[0] + value;
            nbArrows++;
            if (value == 10) {
                ret[2] = ret[2] + 1;
            } else if (value == 9) {
                ret[3] = ret[3] + 1;
            } else if (value == 8) {
                ret[4] = ret[4] + 1;
            }

            if (i < nbArrowsbyShot) {
                ret[5] = ret[5] + value;
            } else if (i < nbArrowsbyShot * 2) {
                ret[6] = ret[6] + value;
            } else if (i < nbArrowsbyShot * 3) {
                ret[7] = ret[7] + value;
            }
        }
        ret[1] = ret[0] / nbArrows;
        DecimalFormat df = new DecimalFormat("##");
        df.setRoundingMode(RoundingMode.DOWN);
        ret[8] = Float.parseFloat(df.format(this.scorecard.size()/nbArrowsbyShoot));
        return ret;

    }

    //Split the whole list to only retreive the scores of a specific score card on String format
    public ArrayList<ArrayList<String>> getScore(int card) {
        ArrayList<ArrayList<String>> ret = new ArrayList();
        ArrayList<String> ret1 = new ArrayList();
        ArrayList<String> ret2 = new ArrayList();

        if (scorecard.size() > 0) {
            int count = 1;
            int nbArrowsbyRound = this.event.getShoot() * this.event.getArrowsByShoot();
            int nbArrowsbyShoot = this.event.getShoot();
            int tempValue = 0;
            int maxTab = 0;
            if (scorecard.size() > (nbArrowsbyRound * (card + 1))) {
                maxTab = (nbArrowsbyRound * (card + 1));
            } else {
                maxTab = scorecard.size();
            }
            for (int l = card * nbArrowsbyRound; l < maxTab; l++) {
                ret1.add(scorecard.get(l).getName());
                tempValue = tempValue + scorecard.get(l).getValue();
                if (count < nbArrowsbyShoot) {
                    ret2.add("");
                    count++;
                } else {
                    ret2.add(tempValue + "");
                    count = 1;
                    tempValue = 0;
                }
            }
        }
        ret.add(ret1);
        ret.add(ret2);
        return ret;

    }

    //Split the whole list to only retreive the impact of a specific score card
    public ArrayList<Impact> getImpacts(int card) {
        ArrayList<Impact> ret = new ArrayList();

        if (scorecard.size() > 0) {
            int nbArrowsbyRound = this.event.getShoot() * this.event.getArrowsByShoot();
            int maxTab = 0;
            if (scorecard.size() > (nbArrowsbyRound * (card + 1))) {
                maxTab = (nbArrowsbyRound * (card + 1));
            } else {
                maxTab = scorecard.size();
            }
            for (int l = card * nbArrowsbyRound; l < maxTab; l++) {
                ret.add(impactcard.get(l));
            }
        }

        return ret;

    }

    public ArrayList<Impact> getImpactcard() {
        return impactcard;
    }
}
