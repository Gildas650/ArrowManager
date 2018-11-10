package fr.arrowm.arrowm.Activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import fr.arrowm.arrowm.Business.Event;
import fr.arrowm.arrowm.Business.Session;
import fr.arrowm.arrowm.Db.ArrowDataBase;
import fr.arrowm.arrowm.R;

public class SessionAct extends AppCompatActivity {

    public static final String PREFERENCES = "ArrowPrefs";
    public static final String BOW_TYPE = "bowType";
    public static final String OBJ_SCE = "objSce";
    public static final String TOL_TIMING = "tolTiming";
    public final static String SESSION = "com.arrowM.SESSION";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_DATA = "sensorData";
    public static final String DELIMITERS = ";";
    public final static String IS_BLUETOOTHON = "com.arrowM.MESSAGE2";
    public final static Integer WAITING_PERIOD = 10;
    public final static Float LOWBAT = 3.3F;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public static String PACKAGE_NAME;
    private Session session;
    private TextView timer;
    private TextView time;
    private Spinner rounds;
    private Spinner chronos;
    private ProgressBar progress;
    private SeekBar seekBar;
    private TextView nbOfArrows;
    private Switch sureToClose;
    private Button closeSession;
    private ImageButton nRound;
    private ImageButton bchrono;
    private ProgressBar progressChrono;
    private ImageView bluestate;
    private ImageView blelowbat;
    private TextView averageTime;
    private TextView minTime;
    private TextView maxTime;
    private TextView lastTime;
    private TextView title3;
    private boolean runTask = true;
    private ArrayList<Event> roundList;
    private ArrayList<String> chronoList = new ArrayList<String>();
    private ArrayList<Integer> chronoTime = new ArrayList<Integer>();
    private ArrowDataBase db = new ArrowDataBase(this);
    private boolean secondRet = false;
    private boolean standbyUpdateNumbersOfArrows = false;
    private boolean displayProgress = false;
    private Integer iniTime = 0;
    private boolean isProgressRunning = false;
    private Date timerD = new Date();
    private Boolean runningflag = false;
    private Boolean goChrono = false;
    private Integer lastBLECount = -1;
    private Integer timeMin = 0;
    private Integer timeMax = 0;
    private Integer timeAvg = 0;
    private Integer timeLast = 0;
    private BarGraphSeries<DataPoint> series;
    private Float tolerance;
    private DecimalFormat df;
    private LocalBroadcastManager localBroadcastManager;
    private AlertDialog.Builder alertDialog;
    private BroadcastReceiver sensorListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> msg = new ArrayList<>();
            msg = splitMessage(intent.getStringExtra(SENSOR_DATA));
            if (msg.size() == 5) {
                //State
                setBLEIcon(Integer.parseInt(msg.get(0)));
                //Count
                if (Integer.parseInt(msg.get(1)) != -1) {
                    addCountfromBLE(Integer.parseInt(msg.get(1)), lastBLECount);
                }
                //Time
                if (Integer.parseInt(msg.get(2)) != -1) {
                    Integer ti = Integer.parseInt(msg.get(2));
                    session.addArrowTime(ti);
                    timeLast = ti;
                    if (ti > timeMax || timeMax == 0) {
                        timeMax = ti;
                    }
                    if (ti < timeMin || timeMin == 0) {
                        timeMin = ti;
                    }
                    Integer total = 0;
                    for (Integer tti : session.getArrowTime()) {
                        total += tti;
                    }
                    timeAvg = total / session.getArrowTime().size();
                    lastTime.setText(df.format((double) timeLast / (double) 1000));
                    minTime.setText(df.format((double) timeMin / (double) 1000));
                    maxTime.setText(df.format((double) timeMax / (double) 1000));
                    averageTime.setText(df.format((double) timeAvg / (double) 1000));
                    renderGraph();

                }
                //Power
                if (Float.parseFloat(msg.get(3)) != -1) {
                    if (Float.parseFloat(msg.get(3)) < LOWBAT) {
                        blelowbat.setVisibility(View.VISIBLE);
                    }
                }
                //Message
                if (!(msg.get(4).equals(" "))) {
                    Log.e("msg", msg.get(4));
                    Log.e("msg", getResources().getIdentifier(msg.get(4), "string", PACKAGE_NAME) + "");
                    if (getResources().getIdentifier(msg.get(4), "string", PACKAGE_NAME) != 0) {
                        Toast.makeText(getApplicationContext(), getResources().getIdentifier(msg.get(4), "string", PACKAGE_NAME), Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(),msg.get(4), Toast.LENGTH_LONG).show();
                    }
                }

                updateSession(session);

            } else {
                Toast.makeText(getApplicationContext(), R.string.error_data + intent.getStringExtra(SENSOR_DATA), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        PACKAGE_NAME = getApplicationContext().getPackageName();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        this.initializeScreen();

        final Intent intent = getIntent();
        if (intent.getSerializableExtra(SESSION) != null) {
            session = (Session) intent.getSerializableExtra(SESSION);
        }
        else if (getLastSession().getEndOfSession() == null){
            alertDialog.show();
        }

        if (intent.getSerializableExtra(IS_BLUETOOTHON) != null) {
            session.setBLEState((Integer) intent.getSerializableExtra(IS_BLUETOOTHON));
        }

        setBLEIcon(session.getBLEState());

        sureToClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sureToClose.isChecked()) {
                    closeSession.setEnabled(true);
                    closeSession.setText("Terminer la session");
                } else {
                    closeSession.setEnabled(false);
                    closeSession.setText("Déverouiller pour terminer");
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int center = 6;
            String sign = "+";

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                standbyUpdateNumbersOfArrows = true;
                if ((progresValue - center) > 0) {
                    sign = "+";
                } else {
                    sign = "";
                }
                nbOfArrows.setText(sign + (progresValue - center) + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                nbOfArrows.setText("");
                if (session.getNumberOfArrows() + (seekBar.getProgress() - center) > 0) {
                    session.setNumberOfArrows(session.getNumberOfArrows() + (seekBar.getProgress() - center));

                    updateSession(session);
                } else {
                    session.setNumberOfArrows(0);
                }
                standbyUpdateNumbersOfArrows = false;
                seekBar.setProgress(center);
                standbyUpdateNumbersOfArrows = false;
            }
        });

        nRound.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                localBroadcastManager.unregisterReceiver(sensorListener);
                if (!session.isExistRound()) {
                    int i = rounds.getSelectedItemPosition();
                    session.setRound(roundList.get(i));
                }
                Intent i = new Intent(SessionAct.this, RoundAct.class);
                i.putExtra(SESSION, session);
                startActivityForResult(i, 0);
                runTask = false;
                finish();
            }
        });

        bchrono.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!displayProgress) {
                    if (!isProgressRunning) {
                        chronos.setVisibility(View.GONE);
                        progressChrono.setVisibility(View.VISIBLE);
                        time.setVisibility(View.VISIBLE);
                        iniTime = chronoTime.get(chronos.getSelectedItemPosition());
                        time.setText(convertTime(iniTime));
                        displayProgress = true;
                        progressChrono.setProgress(0);
                        progressChrono.setMax(iniTime);

                    }
                } else {
                    if (!isProgressRunning) {
                        bchrono.setImageResource(R.drawable.ret);
                        timerD = new Date();
                        timerD.setTime(timerD.getTime() + (iniTime * 1000) + (WAITING_PERIOD * 1000));
                        progressChrono.getProgressDrawable().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.pred, null), PorterDuff.Mode.SRC_IN);
                        progressChrono.setProgress(0);
                        isProgressRunning = true;
                        //Launch timer
                        //
                    } else {
                        //Restart timer
                        bchrono.setImageResource(R.drawable.right);
                        progressChrono.setProgress(0);
                        progressChrono.getProgressDrawable().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.pred, null), PorterDuff.Mode.SRC_IN);
                        time.setText(convertTime(iniTime));
                        runningflag = false;
                        goChrono = false;
                        isProgressRunning = false;

                    }
                }


            }
        });

        progressChrono.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                progressChrono.setVisibility(View.GONE);
                time.setVisibility(View.INVISIBLE);
                chronos.setVisibility(View.VISIBLE);
                bchrono.setImageResource(R.drawable.right);
                displayProgress = false;
                runningflag = false;
                goChrono = false;
                progressChrono.setProgress(0);
                isProgressRunning = false;
            }
        });

        closeSession.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (sureToClose.isChecked()) {
                    localBroadcastManager.unregisterReceiver(sensorListener);
                    runTask = false;
                    session.closeSession();
                    if (session.getNumberOfArrows() > 0) {
                        updateSession(session);
                    }
                    finish();
                }
            }
        });

        initializeGraph();

        initializeTime();

        localBroadcastManager.registerReceiver(sensorListener,
                new IntentFilter(SENSOR));
    }

    @Override
    public void onBackPressed() {
         if (secondRet || session.getNumberOfArrows() == 0) {
            runTask = false;
            finish();
        } else {
            //Toast.makeText(getApplicationContext(), "Attention, la session ne sera pas sauvegardée !!!, Appuyez a nouveau sur retour pour continuer", Toast.LENGTH_LONG).show();
            secondRet = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        runTask = true;
        updateView uv = new updateView();
        uv.execute();

        if (session.isExistRound()) {
            for (int ev = 0; ev < roundList.size(); ev++) {
                if (session.getRound().getEvent().getId() == roundList.get(ev).getId()) {
                    rounds.setSelection(ev);
                }
            }
            rounds.setEnabled(false);
        }

        secondRet = false;

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initializeScreen() {

        this.session = new Session();
        this.roundList = new ArrayList<>();

        df = new DecimalFormat("##0.00");

        SharedPreferences sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        for (int i = 0; i < Event.values().length; i++) {
            if (Event.values()[i].getRc().equals(sharedpreferences.getString(BOW_TYPE, ""))) {
                this.roundList.add(Event.values()[i]);
            }
        }
        tolerance = Float.parseFloat(sharedpreferences.getString(TOL_TIMING, "1"));

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        nbOfArrows = (TextView) findViewById(R.id.nbOfArrows);
        timer = (TextView) findViewById(R.id.timer);
        time = (TextView) findViewById(R.id.time);
        nRound = (ImageButton) findViewById(R.id.nRound);
        sureToClose = (Switch) findViewById(R.id.sureToClose);
        closeSession = (Button) findViewById(R.id.closeSession);
        progress = (ProgressBar) findViewById(R.id.progress);
        progress.setMax(Integer.parseInt(sharedpreferences.getString(OBJ_SCE, "100")));
        rounds = (Spinner) findViewById(R.id.round);
        chronos = (Spinner) findViewById(R.id.chronoSelect);
        bchrono = (ImageButton) findViewById(R.id.bchrono);
        progressChrono = (ProgressBar) findViewById(R.id.progressChrono);
        bluestate = (ImageView) findViewById(R.id.bluestate);
        blelowbat = (ImageView) findViewById(R.id.lowblebat);
        averageTime = (TextView) findViewById(R.id.averageTime);
        minTime = (TextView) findViewById(R.id.minTime);
        maxTime = (TextView) findViewById(R.id.maxTime);
        lastTime = (TextView) findViewById(R.id.lastTime);
        title3 = (TextView) findViewById(R.id.title3);

        // Chronos label
        chronoList.add("4 minutes - 6 flèches");
        chronoList.add("2 minutes - 3 flèches");
        chronoList.add("1 minute - 3 flèches");
        chronoList.add("20 secondes - 1 flèche");

        // Chronos label
        chronoTime.add(240);
        chronoTime.add(120);
        chronoTime.add(60);
        chronoTime.add(20);

        ArrayAdapter<Event> adapter = new ArrayAdapter<Event>(this,
                R.layout.spinner_layout, roundList);
        adapter.setDropDownViewResource(R.layout.spinner_layout);
        rounds.setAdapter(adapter);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
                R.layout.spinner_layout, chronoList);
        adapter2.setDropDownViewResource(R.layout.spinner_layout);
        chronos.setAdapter(adapter2);

        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.dialog_title);
        alertDialog.setMessage(R.string.dialog_texte);
        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        db.open();
                        session = db.selectLast().get(0);
                        db.close();
                    }
                });
        alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);

    }

    private void initializeGraph() {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        graph.getGridLabelRenderer().setGridColor(ResourcesCompat.getColor(getResources(), R.color.Grey, null));
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(true);
        graph.getGridLabelRenderer().setVerticalLabelsColor(ResourcesCompat.getColor(getResources(), R.color.darkGrey, null));
        graph.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.CENTER);
        graph.getGridLabelRenderer().setTextSize(30);

        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(16);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(15);

        series = new BarGraphSeries<>();
        graph.addSeries(series);

        series.setSpacing(20);

// draw values on top
        series.setDrawValuesOnTop(false);

        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                //Afficher valeur on click
                displayToastBelowView(title3, dataPoint.getY() + "");
            }
        });

        series.setValueDependentColor(new ValueDependentColor<DataPoint>() {
            @Override
            public int get(DataPoint data) {
                //Log.e("Debug",data.getY() + "");
                Integer ret;
                if (data.getY() > ((double) timeAvg / (double) 1000) + tolerance) {
                    ret = ResourcesCompat.getColor(getResources(), R.color.compGreen, null);
                } else if (data.getY() < ((double) timeAvg / (double) 1000) - tolerance) {
                    ret = ResourcesCompat.getColor(getResources(), R.color.compGreen, null);
                } else {
                    ret = ResourcesCompat.getColor(getResources(), R.color.pgreen, null);
                }
                return ret;
            }
        });
    }

    private void initializeTime() {
        if (session.getArrowTime().size() > 0) {
            Integer total = 0;
            for (int at = 0; at < session.getArrowTime().size(); at++) {
                if (at == 0) {
                    timeMax = session.getArrowTime().get(at);
                    timeMin = session.getArrowTime().get(at);
                } else {
                    if (session.getArrowTime().get(at) < timeMin) {
                        timeMin = session.getArrowTime().get(at);
                    }
                    if (session.getArrowTime().get(at) > timeMax) {
                        timeMax = session.getArrowTime().get(at);
                    }
                }
                timeLast = session.getArrowTime().get(at);
                total += session.getArrowTime().get(at);
            }
            timeAvg = total / session.getArrowTime().size();
            lastTime.setText(df.format((double) timeLast / (double) 1000));
            minTime.setText(df.format((double) timeMin / (double) 1000));
            maxTime.setText(df.format((double) timeMax / (double) 1000));
            averageTime.setText(df.format((double) timeAvg / (double) 1000));
        }
        renderGraph();
    }

    private String[] getTimer() {
        String[] ret = new String[2];

        Date fin = new Date();

        long diff = timerD.getTime() - fin.getTime();
        int d = (int) (diff / 1000);

        ret[0] = convertTime(diff);
        ret[1] = (iniTime - d) + "";
        return ret;
    }

    private String convertTime(int sec) {
        String sm;
        String ss;
        String ret = new String();
        int s = sec % 60;
        int m = (sec - s) / 60;

        if (m < 0) {
            sm = "00";
        } else {
            if (m < 10) {
                sm = "0" + m;
            } else {
                sm = m + "";
            }
        }

        if (s < 0) {
            ss = "00";
        } else {
            if (s < 10) {
                ss = "0" + s;
            } else {
                ss = s + "";
            }
        }
        ret = sm + ":" + ss;
        return ret;
    }

    private String convertTime(float sec) {
        String sm;
        String ss;
        String ret = new String();
        int s = (int) (sec / 1000) % 60;
        int m = (int) ((sec / (1000 * 60)) % 60);

        if (m < 0) {
            sm = "00";
        } else {
            if (m < 10) {
                sm = "0" + m;
            } else {
                sm = m + "";
            }
        }

        if (s < 0) {
            ss = "00";
        } else {
            if (s < 10) {
                ss = "0" + s;
            } else {
                ss = s + "";
            }
        }
        ret = sm + ":" + ss;
        return ret;
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

    private void displayToastBelowView(View v, String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, v.getTop() + v.getMeasuredHeight());
        toast.show();
    }

    private void setBLEIcon(int state) {
        if (state == STATE_CONNECTED) {
            bluestate.setImageResource(R.drawable.ic_bluetooth);
        } else if (state == STATE_CONNECTING) {
            bluestate.setImageResource(R.drawable.ic_bluetoothwait);
        } else {
            bluestate.setImageResource(R.drawable.ic_bluetoothoff);
        }
    }

    public void renderGraph() {
        ArrayList<DataPoint> dps = new ArrayList<>();
        dps.add(new DataPoint(0, 0));
        if (session.getArrowTime().size() > 0) {
            for (int at = 0; at < session.getArrowTime().size(); at++) {
                dps.add(new DataPoint(at + 1, (double) session.getArrowTime().get(at) / (double) 1000));
            }
        }
        DataPoint ret[] = new DataPoint[session.getArrowTime().size()];

        series.resetData(dps.toArray(ret));
    }

    private class updateView extends AsyncTask {
        Integer step = 0;

        @Override
        protected Object doInBackground(Object[] params) {
            while (runTask) {
                if (isProgressRunning) {
                    if (!runningflag) {
                        step = WAITING_PERIOD * 10;
                        runningflag = true;
                    }
                    step = step - 1;
                    if (step == 0) {
                        goChrono = true;
                    }
                    if (goChrono) {
                        String val[] = {session.toStringSessionDuration(), session.getNumberOfArrows() + "", getTimer()[0], getTimer()[1]};
                        publishProgress(val);
                    } else {
                        String val[] = {session.toStringSessionDuration(), session.getNumberOfArrows() + "", convertTime(step / 10), "0"};
                        publishProgress(val);
                    }
                } else {
                    String val[] = {session.toStringSessionDuration(), session.getNumberOfArrows() + "", getTimer()[0], getTimer()[1]};
                    publishProgress(val);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            timer.setText(values[0] + "");
            if (!standbyUpdateNumbersOfArrows) {
                nbOfArrows.setText(values[1] + "");
            }
            if (isProgressRunning) {
                if (Integer.parseInt(values[3] + "") == (iniTime - 30)) {
                    progressChrono.getProgressDrawable().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.pyellow, null), PorterDuff.Mode.SRC_IN);
                } else if (Integer.parseInt(values[3] + "") == iniTime) {
                    isProgressRunning = false;
                    runningflag = false;
                    goChrono = false;
                    bchrono.setImageResource(R.drawable.right);
                    progressChrono.getProgressDrawable().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.pred, null), PorterDuff.Mode.SRC_IN);
                    time.setText(convertTime(iniTime));
                    isProgressRunning = false;
                }
                if (step == 0 && runningflag) {
                    progressChrono.getProgressDrawable().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.pgreen, null), PorterDuff.Mode.SRC_IN);
                }
                time.setText(values[2] + "");
                progressChrono.setProgress(Integer.parseInt(values[3] + ""));
            }
            progress.setProgress(Integer.parseInt(values[1] + ""));
        }

    }
    private ArrayList<String> splitMessage(String msg) {
        StringTokenizer strTkn = new StringTokenizer(msg, DELIMITERS);
        ArrayList<String> arrLis = new ArrayList<>(msg.length());

        while (strTkn.hasMoreTokens())
            arrLis.add(strTkn.nextToken());

        return arrLis;
    }

    private void updateSession(Session s){
        db.open();
        if (s.getDbId() == -1){
            db.insert(s);
        }
        else{
            db.removeSession(s.getDbId());
            db.insert(s);
        }

        db.close();
    }

    private Session getLastSession() {
        db.open();
        Session s = db.selectLast().get(0);
        db.close();

        return s;
    }

}
