package fr.arrowm.arrowm.Business;

import java.io.Serializable;

/**
 * Class used to store impact on touchview
 * X an Y are the coordinate of the impact
 * offsetC : Is the offset between the finger and the point on touchview (must be changed in touchable Image View too)
 * pointsize : Is the size of the point (must be changed in touchable Image View too)
 * Created by Gildas on 18/12/2016.
 */

public class Impact implements Serializable {

    private float x;
    private float y;

    public Impact(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    void setY(float y) {
        this.y = y;
    }

    public Double getDistance(float centerX, float centerY) {
        Double ret = Math.sqrt(Math.pow(Math.abs(centerX - x), 2) + Math.pow(Math.abs(centerY - (y - Constants.IMPACT.OFFSETC)), 2));
        ret = ret - (Constants.IMPACT.POINTSIZE * 2);
        return ret;
    }
}
