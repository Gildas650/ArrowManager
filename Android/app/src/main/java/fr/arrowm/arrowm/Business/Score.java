package fr.arrowm.arrowm.Business;

import java.util.ArrayList;

/**
 * Class used to give all parameter of a score
 * ID : index in DB
 * Name : Name to display on screen
 * Value : Value of the score
 * Created by Gildas on 13/12/2016.
 */

public enum Score {
    SX(0, "X", 10),
    S10(1, "10", 10),
    S9(2, "9", 9),
    S8(3, "8", 8),
    S7(4, "7", 7),
    S6(5, "6", 6),
    S5(6, "5", 5),
    S4(7, "4", 4),
    S3(8, "3", 3),
    S2(9, "2", 2),
    S1(10, "1", 1),
    SM(11, "M", 0);


    private int id = 0;
    private String name = "";
    private int value = 0;

    Score(int id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    //Method which give the scores to use for Trispot (Score can be define twice to manage "little 10" or X)
    public ArrayList<Score> getTriSpotList() {
        ArrayList<Score> ret = new ArrayList<>();
        ret.add(Score.S10);
        ret.add(Score.S9);
        ret.add(Score.S9);
        ret.add(Score.S9);
        ret.add(Score.S8);
        ret.add(Score.S8);
        ret.add(Score.S7);
        ret.add(Score.S7);
        ret.add(Score.S6);
        ret.add(Score.S6);
        ret.add(Score.SM);
        return ret;
    }

    //Method which give the scores to use for Compound Spot (Score can be define twice to manage "little 10" or X)
    public ArrayList<Score> getSpotList() {
        ArrayList<Score> ret = new ArrayList<>();
        ret.add(Score.SX);
        ret.add(Score.S10);
        ret.add(Score.S9);
        ret.add(Score.S9);
        ret.add(Score.S8);
        ret.add(Score.S8);
        ret.add(Score.S7);
        ret.add(Score.S7);
        ret.add(Score.S6);
        ret.add(Score.S6);
        ret.add(Score.S5);
        ret.add(Score.S5);
        ret.add(Score.SM);
        return ret;
    }

    //Method which give the scores to use for Standard spot (Score can be define twice to manage "little 10" or X)
    public ArrayList<Score> getStandardList() {
        ArrayList<Score> ret = new ArrayList<>();
        ret.add(Score.SX);
        ret.add(Score.S10);
        ret.add(Score.S9);
        ret.add(Score.S9);
        ret.add(Score.S8);
        ret.add(Score.S8);
        ret.add(Score.S7);
        ret.add(Score.S7);
        ret.add(Score.S6);
        ret.add(Score.S6);
        ret.add(Score.S5);
        ret.add(Score.S5);
        ret.add(Score.S4);
        ret.add(Score.S4);
        ret.add(Score.S3);
        ret.add(Score.S3);
        ret.add(Score.S2);
        ret.add(Score.S2);
        ret.add(Score.S1);
        ret.add(Score.S1);
        ret.add(Score.SM);
        return ret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public int getId() {
        return id;
    }
}
