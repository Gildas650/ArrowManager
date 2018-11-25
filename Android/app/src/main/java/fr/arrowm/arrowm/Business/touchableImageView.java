package fr.arrowm.arrowm.Business;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import fr.arrowm.arrowm.R;

/**
 * A Specific View to managed the spot in ROund ACtivity and add the impacts
 * offsetC : Is the offset between the finger and the point on touchview (must be changed in Impact too)
 * pointsize : Is the size of the point (must be changed in Impact too)
 * allowTo draw define if we can draw on the session (used to block the touch event in case of consult)
 * Created by Gildas on 18/12/2016.
 */

public class touchableImageView extends android.support.v7.widget.AppCompatImageView {

    List<Impact> points = new ArrayList<>();
    Paint paint = new Paint();
    int offsetC = 150;
    int pointSize = 15;
    Context context;
    boolean allowedToDraw = true;

    public touchableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    //Draw all the Impact in the list on each invalidate call
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int myColor = context.getResources().getColor(R.color.point);
        paint.setColor(myColor);
        for (Impact point : points) {
            canvas.drawCircle(point.getX(), point.getY() - offsetC, pointSize, paint);
        }
    }

    @Override
    //OnTOuch Event oonly for visual and not for storing data
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (allowedToDraw) {
            Impact point = new Impact(0, 0);
            final int action = event.getAction();
            switch (action) {
                //Impact is created when we touch the screen
                case MotionEvent.ACTION_DOWN:
                    point.setX(event.getX());
                    point.setY(event.getY());
                    points.add(point);
                    //If finger move on the screen we update the point
                case MotionEvent.ACTION_MOVE:
                    Impact pointU = points.get(points.size() - 1);
                    pointU.setX(event.getX());
                    pointU.setY(event.getY());

                case MotionEvent.ACTION_UP:

            }
            /* Trigger the onDraw method */
            invalidate();
        }
        return true;
    }

    public List<Impact> getPoints() {
        return points;
    }

    public void setPoints(List<Impact> points) {
        this.points = points;
    }

    public void setAllowedToDraw(boolean allowedToDraw) {
        this.allowedToDraw = allowedToDraw;
    }
}