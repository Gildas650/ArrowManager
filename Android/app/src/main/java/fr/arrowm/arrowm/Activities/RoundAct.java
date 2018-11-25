package fr.arrowm.arrowm.Activities;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;
import fr.arrowm.arrowm.Business.Impact;
import fr.arrowm.arrowm.Business.Score;
import fr.arrowm.arrowm.Business.Session;
import fr.arrowm.arrowm.Business.touchableImageView;
import fr.arrowm.arrowm.Db.ArrowDataBase;
import fr.arrowm.arrowm.R;

public class RoundAct extends AppCompatActivity {

    public final static String SESSION = "com.arrowM.SESSION";
    public final static String TRI_SPOT = "FITA Tri-Spot";
    public final static String SPOT = "FITA Mono-Spot";
    public final static String STANDARD = "FITA";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_DATA = "sensorData";
    public static final String DELIMITERS = ";";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public static String PACKAGE_NAME;
    private Session session;
    private touchableImageView face;
    private Button ret;
    private ImageButton cancelA;
    private Button s1b;
    private Button s2b;
    private Button s3b;
    private TextView s1;
    private TextView s2;
    private TextView s3;
    private TextView total;
    private TextView vol;
    private TextView moy;
    private TextView nb10;
    private TextView nb9;
    private TextView nb8;
    private TableRow aRow;
    private TableRow sumR;
    private HorizontalScrollView scrlScore;
    private int faceSize;
    private int circleNumber;
    private float centerPos;
    private float zoneSize;
    private String faceType;
    private TextView instantScore;
    private int arrowsRemaining;
    private ArrayList<ArrayList<String>> aas = new ArrayList();
    private ArrayList<TextView> scoreTable = new ArrayList<>();
    private ArrayList<TextView> sumTable = new ArrayList<>();
    private ArrayList<Score> scoreToUse;
    private Integer lastBLECount = -1;
    private Boolean useBLE = false;
    private LocalBroadcastManager localBroadcastManager;
    private ArrowDataBase db = new ArrowDataBase(this);
    private BroadcastReceiver sensorListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> msg = new ArrayList<>();
            msg = splitMessage(intent.getStringExtra(SENSOR_DATA));
            if (Integer.parseInt(msg.get(0)) == STATE_CONNECTED) {
                session.setBLEState(STATE_CONNECTED);
                useBLE = true;
            } else if (Integer.parseInt(msg.get(0)) == STATE_CONNECTING) {
                session.setBLEState(STATE_CONNECTING);
                useBLE = true;
            } else {
                session.setBLEState(STATE_DISCONNECTED);
            }
            if (Integer.parseInt(msg.get(1)) != -1) {
                addCountfromBLE(Integer.parseInt(msg.get(1)), lastBLECount);
            }
            //Time
            if (Integer.parseInt(msg.get(2)) != -1) {
                session.addArrowTime(Integer.parseInt(msg.get(2)));
            }
            if (!(msg.get(4).equals(" "))) {
                Log.e("msg", msg.get(4));
                if (getResources().getIdentifier(msg.get(4), "string", PACKAGE_NAME) != 0) {
                    Toast.makeText(getApplicationContext(), getResources().getIdentifier(msg.get(4), "string", PACKAGE_NAME), Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(getApplicationContext(),msg.get(4), Toast.LENGTH_LONG).show();
                }
            }
            updateSession(session,true);
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_round);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        PACKAGE_NAME = getApplicationContext().getPackageName();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        initializeScreen();

        if (session.getBLEState() == STATE_DISCONNECTED) {
            useBLE = false;
        } else {
            useBLE = true;
        }

        ret.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                localBroadcastManager.unregisterReceiver(sensorListener);
                face.setAllowedToDraw(true);
                Intent i = new Intent(RoundAct.this, SessionAct.class);
                i.putExtra(SESSION, session);
                startActivityForResult(i, 0);
                finish();
            }
        });

        cancelA.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (face.getPoints().size() > 0) {
                    face.getPoints().remove(face.getPoints().size() - 1);
                }
                face.invalidate();
                removeArrowImpact();
                manageRoundBar();
                manageView();
            }
        });

        s1b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                aas = session.getRound().getScore(0);
                face.setPoints(session.getRound().getImpacts(0));
                face.invalidate();
                displayScoreTab();
            }
        });


        s2b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                aas = session.getRound().getScore(1);
                face.setPoints(session.getRound().getImpacts(1));
                face.invalidate();
                displayScoreTab();
            }
        });


        s3b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                aas = session.getRound().getScore(2);
                face.setPoints(session.getRound().getImpacts(2));
                face.invalidate();
                displayScoreTab();
            }
        });


        face.setOnTouchListener(new View.OnTouchListener() {
                                    public boolean onTouch(View myView, MotionEvent event) {
                                        if (session.getEndOfSession() == null) {
                                            int action = event.getAction();
                                            if (action == MotionEvent.ACTION_DOWN) {
                                                cancelA.setVisibility(View.INVISIBLE);
                                                instantScore.setVisibility(View.VISIBLE);
                                            }
                                            if (action == MotionEvent.ACTION_UP) {
                                                cancelA.setVisibility(View.VISIBLE);
                                                instantScore.setVisibility(View.INVISIBLE);
                                                if (arrowsRemaining > 0) {
                                                    Score s = scoretoUse();
                                                    Impact imp = new Impact(event.getX(), event.getY());
                                                    addArrowImpact(s, imp);
                                                } else {
                                                    if (face.getPoints().size() > 0) {
                                                        face.getPoints().remove(face.getPoints().size() - 1);
                                                    }
                                                }
                                                manageRoundBar();
                                                manageView();
                                            }
                                            if (action == MotionEvent.ACTION_MOVE) {
                                                instantScore.setText(scoretoUse().getName());
                                            }
                                        }
                                        return false;
                                    }
                                }
        );

        PACKAGE_NAME = getApplicationContext().getPackageName();

        localBroadcastManager.registerReceiver(sensorListener,
                new IntentFilter(SENSOR));
    }

    @Override
    public void onBackPressed() {
        //
        if (session.getEndOfSession() == null) {
            face.setAllowedToDraw(true);
            Intent i = new Intent(RoundAct.this, SessionAct.class);
            i.putExtra(SESSION, session);
            startActivityForResult(i, 0);
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session.getRound().getEvent().getSpot().equals(TRI_SPOT)) {
            circleNumber = Score.values()[0].getTriSpotList().size() - 1;
            faceType = TRI_SPOT;
            face.setImageResource(R.drawable.ictarget);
            scoreToUse = Score.values()[0].getTriSpotList();
        } else if (session.getRound().getEvent().getSpot().equals(SPOT)) {
            circleNumber = Score.values()[0].getSpotList().size() - 1;
            faceType = SPOT;
            face.setImageResource(R.drawable.octarget);
            scoreToUse = Score.values()[0].getSpotList();
        } else {
            circleNumber = Score.values()[0].getStandardList().size() - 1;
            faceType = STANDARD;
            face.setImageResource(R.drawable.starget);
            scoreToUse = Score.values()[0].getStandardList();
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        faceSize= (int) (displayMetrics.widthPixels * 0.95);
        FrameLayout.LayoutParams fllp =  new FrameLayout.LayoutParams(faceSize, faceSize);
        fllp.gravity = Gravity.CENTER_HORIZONTAL;
        face.setLayoutParams(fllp);


        manageRoundBar();
        manageView();
        if (session.getEndOfSession() != null) {
            face.setAllowedToDraw(false);
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //faceSize = face.getHeight();
        //faceSize = face.getMeasuredHeight();
        centerPos = faceSize / 2;
        zoneSize = centerPos / circleNumber;
    }

    public Score scoretoUse() {
        Score ret;
        Double distance = face.getPoints().get(face.getPoints().size() - 1).getDistance(centerPos, centerPos);
        int index = (int) Math.round(distance / zoneSize);

        if (index == -1) {
            index = 0;
        }
        if (index < scoreToUse.size()) {
            ret = scoreToUse.get(index);
        } else {
            ret = scoreToUse.get(scoreToUse.size() - 1);
        }
        return ret;
    }

    public void manageRoundBar() {
        switch (session.getRound().getNumberOfScoreCard()) {
            case 0:
                s1b.setVisibility(View.VISIBLE);
                s2b.setVisibility(View.INVISIBLE);
                s3b.setVisibility(View.INVISIBLE);
                s1.setVisibility(View.VISIBLE);
                s2.setVisibility(View.INVISIBLE);
                s3.setVisibility(View.INVISIBLE);
                aas = session.getRound().getScore(0);
                face.setPoints(session.getRound().getImpacts(0));
                break;
            case 1:
                s1b.setVisibility(View.VISIBLE);
                s2b.setVisibility(View.VISIBLE);
                s3b.setVisibility(View.INVISIBLE);
                s1.setVisibility(View.VISIBLE);
                s2.setVisibility(View.VISIBLE);
                s3.setVisibility(View.INVISIBLE);
                aas = session.getRound().getScore(1);
                face.setPoints(session.getRound().getImpacts(1));
                break;
            case 2:
                s1b.setVisibility(View.VISIBLE);
                s2b.setVisibility(View.VISIBLE);
                s3b.setVisibility(View.VISIBLE);
                s1.setVisibility(View.VISIBLE);
                s2.setVisibility(View.VISIBLE);
                s3.setVisibility(View.VISIBLE);
                aas = session.getRound().getScore(2);
                face.setPoints(session.getRound().getImpacts(2));
                break;
            default:
                s1b.setVisibility(View.VISIBLE);
                s2b.setVisibility(View.VISIBLE);
                s3b.setVisibility(View.VISIBLE);
                s1.setVisibility(View.VISIBLE);
                s2.setVisibility(View.VISIBLE);
                s3.setVisibility(View.VISIBLE);
                aas = session.getRound().getScore(3);
                face.setPoints(session.getRound().getImpacts(3));
                break;
        }
        displayScoreTab();
        scrlScore.scrollTo(5000, 5000);

    }

    public void manageView() {
        float sessionStat[] = session.getRound().getStats();
        DecimalFormat df = new DecimalFormat("0.00");
        DecimalFormat dg = new DecimalFormat("000");
        DecimalFormat dh = new DecimalFormat("00");
        total.setText(dg.format(sessionStat[0]));
        vol.setText(dh.format(sessionStat[8]));
        s1.setText(dg.format(sessionStat[5]));
        s2.setText(dg.format(sessionStat[6]));
        s3.setText(dg.format(sessionStat[7]));
        moy.setText(df.format(sessionStat[1]));
        nb10.setText(dh.format(sessionStat[2]));
        nb9.setText(dh.format(sessionStat[3]));
        nb8.setText(dh.format(sessionStat[4]));
    }

    public void addArrowImpact(Score s, Impact imp) {
        if (!useBLE) {
            session.addArrow();
        }
        session.getRound().addArrowScore(s, imp);
        updateSession(session, true);
        arrowsRemaining--;

    }

    public void removeArrowImpact() {
        if (!useBLE) {
            session.removeArrow();
        }
        session.getRound().removeArrowScore();
        if (arrowsRemaining < (3 * (session.getRound().getEvent().getArrowsByShoot() * session.getRound().getEvent().getShoot())) - 1) {
            arrowsRemaining++;
        }
        updateSession(session,true);
    }

    private void initializeScreen() {
        final Intent intent = getIntent();
        session = (Session) intent.getSerializableExtra(SESSION);

        ret = (Button) findViewById(R.id.returnc);
        cancelA = (ImageButton) findViewById(R.id.cancelA);
        if (session.getEndOfSession() != null) {
            ret.setVisibility(View.INVISIBLE);
            cancelA.setVisibility(View.INVISIBLE);
        }
        face = (touchableImageView) findViewById(R.id.targetface);
        instantScore = (TextView) findViewById(R.id.instantScore);
        instantScore.setVisibility(View.INVISIBLE);
        s1b = (Button) findViewById(R.id.s1b);
        s2b = (Button) findViewById(R.id.s2b);
        s3b = (Button) findViewById(R.id.s3b);
        s1 = (TextView) findViewById(R.id.s1);
        s2 = (TextView) findViewById(R.id.s2);
        s3 = (TextView) findViewById(R.id.s3);
        total = (TextView) findViewById(R.id.total);
        moy = (TextView) findViewById(R.id.moy);
        vol = (TextView) findViewById(R.id.vol);
        nb10 = (TextView) findViewById(R.id.nb10);
        nb9 = (TextView) findViewById(R.id.nb9);
        nb8 = (TextView) findViewById(R.id.nb8);
        aRow = (TableRow) findViewById(R.id.aRow);
        sumR = (TableRow) findViewById(R.id.sumR);
        scrlScore = (HorizontalScrollView) findViewById(R.id.scrlScore);
        int tableSize = session.getRound().getEvent().getArrowsByShoot() * session.getRound().getEvent().getShoot();
        arrowsRemaining = (3 * (tableSize)) - session.getRound().getNumberOfArrows();
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, 0, 8, 0);

        for (int i = 0; i < tableSize; i++) {
            TextView t = new TextView(this);
            t.setTextColor(ResourcesCompat.getColor(getResources(), R.color.darkGrey, null));
            scoreTable.add(t);
            scoreTable.get(i).setLayoutParams(params);
            aRow.addView(scoreTable.get(i));
            TextView t2 = new TextView(this);
            t2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.darkGrey, null));
            sumTable.add(t2);
            sumTable.get(i).setLayoutParams(params);
            sumR.addView(sumTable.get(i));
        }


    }

    public void reinitScoreCard() {
        for (int i = 0; i < scoreTable.size(); i++) {
            scoreTable.get(i).setText("");
            sumTable.get(i).setText("");
        }

    }

    public void displayScoreTab() {
        String tmp1;
        String tmp2;
        reinitScoreCard();
        for (int j = 0; j < aas.get(0).size(); j++) {
            tmp1 = aas.get(0).get(j);
            tmp2 = aas.get(1).get(j);
            scoreTable.get(j).setText(tmp1);
            sumTable.get(j).setText(tmp2);
        }
    }

    private void addCountfromBLE(Integer BLECount, Integer lastBLECount) {
        if (lastBLECount == -1) {
            this.lastBLECount = BLECount;
            session.addArrow();
        } else {
            if (BLECount > lastBLECount) {
                for (int i = 0; i < BLECount - lastBLECount; i++) {
                    session.addArrow();
                }
            } else if (BLECount < lastBLECount) {
                session.addArrow();
            }
            this.lastBLECount = BLECount;
        }
    }

    private ArrayList<String> splitMessage(String msg) {
        StringTokenizer strTkn = new StringTokenizer(msg, DELIMITERS);
        ArrayList<String> arrLis = new ArrayList<>(msg.length());

        while (strTkn.hasMoreTokens())
            arrLis.add(strTkn.nextToken());

        return arrLis;
    }

    private void updateSession(Session s, Boolean isTemp){
        db.open();
        if(isTemp){
            db.dropTmp();
        }
        db.insert(s,isTemp);
        db.close();
    }
}
