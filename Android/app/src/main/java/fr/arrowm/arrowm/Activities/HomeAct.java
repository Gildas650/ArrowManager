package fr.arrowm.arrowm.Activities;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.StringTokenizer;

import fr.arrowm.arrowm.Business.Constants;
import fr.arrowm.arrowm.Business.SimpleCircularProgressbar;
import fr.arrowm.arrowm.Db.ArrowDataBase;
import fr.arrowm.arrowm.R;
import fr.arrowm.arrowm.Service.BLESensor;


public class HomeAct extends AppCompatActivity {
    /*public final static String IS_BLUETOOTHON = "com.arrowM.MESSAGE2";
    public static final String PREFERENCES = "ArrowPrefs";
    public static final String OBJ_WEEK = "objWeek";
    public static final String OBJ_MONTH = "objMonth";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_DATA = "sensorData";
    public static final String DELIMITERS = ";";
    public static final String STARTFOREGROUND_ACTION ="fr.arrowm.arrowm.action.startforeground";
    public static final String STOPFOREGROUND_ACTION ="fr.arrowm.arrowm.action.stopforeground";
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;*/
    public static String PACKAGE_NAME;
    private SharedPreferences sharedpreferences;
    private TextView weeklytext;
    private TextView monthlytext;
    private SimpleCircularProgressbar weeklyprogress;
    private SimpleCircularProgressbar monthlyprogress;
    private ArrowDataBase db = new ArrowDataBase(this);
    private boolean bluetoothOn = false;
    private Menu menu;
    private Intent bleService;
    private int bleState;
    private BroadcastReceiver sensorListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            MenuItem item = menu.findItem(R.id.launch_service);
            ArrayList<String> msg = new ArrayList<>();
            msg = splitMessage(intent.getStringExtra(Constants.DECL.SENSOR_DATA));
            if (Integer.parseInt(msg.get(0)) == Constants.BLE.STATE_CONNECTED) {
                bleState = Constants.BLE.STATE_CONNECTED;
                item.setIcon(R.drawable.ic_bluetooth);
            } else if (Integer.parseInt(msg.get(0)) == Constants.BLE.STATE_CONNECTING) {
                bleState = Constants.BLE.STATE_CONNECTING;
                item.setIcon(R.drawable.ic_bluetoothwait);
            } else {
                bleState =Constants.BLE.STATE_DISCONNECTED;
                item.setIcon(R.drawable.ic_bluetoothoff);
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PACKAGE_NAME = getApplicationContext().getPackageName();
        setContentView(R.layout.activity_home);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        weeklytext = (TextView) findViewById(R.id.weeklytext);
        monthlytext = (TextView) findViewById(R.id.monthlytext);
        weeklyprogress = (SimpleCircularProgressbar) findViewById(R.id.weeklyprogress);
        monthlyprogress = (SimpleCircularProgressbar) findViewById(R.id.monthlyprogress);
        sharedpreferences = getSharedPreferences(Constants.DECL.PREFERENCES, Context.MODE_PRIVATE);

        final ImageButton nTraining = (ImageButton) findViewById(R.id.TrainingButton);
        nTraining.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(HomeAct.this, SessionAct.class);
                i.putExtra(Constants.DECL.IS_BLUETOOTHON, bleState);
                startActivityForResult(i, 0);
            }
        });

        final ImageButton scoreButton = (ImageButton) findViewById(R.id.scoreButton);
        scoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(HomeAct.this, LogAct.class);
                startActivity(i);
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(sensorListener,
                new IntentFilter(Constants.DECL.SENSOR));

        bleService = new Intent(HomeAct.this, BLESensor.class);
    }

    @Override

    public void onStart() {

        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        db.open();
        ArrayList<String> dObj = db.defineObjResult();
        db.close();
        if (dObj.get(0) == null) {
            dObj.set(0, "0");
        }
        if (dObj.get(1) == null) {
            dObj.set(1, "0");
        }
        weeklytext.setText(dObj.get(0) + "/" + sharedpreferences.getString(Constants.DECL.OBJ_WEEK, "500"));
        monthlytext.setText(dObj.get(1) + "/" + sharedpreferences.getString(Constants.DECL.OBJ_MONTH, "2000"));
        weeklyprogress.setProgress((Float.parseFloat(dObj.get(0)) / Float.parseFloat(sharedpreferences.getString(Constants.DECL.OBJ_WEEK, "500"))) * 100);
        monthlyprogress.setProgress((Float.parseFloat(dObj.get(1)) / Float.parseFloat(sharedpreferences.getString(Constants.DECL.OBJ_MONTH, "2000"))) * 100);

        LocalBroadcastManager.getInstance(this).registerReceiver(sensorListener,
                new IntentFilter(Constants.DECL.SENSOR));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //ajoute les entrées de menu_test à l'ActionBar
        this.menu = menu;
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.launch_service:
                // Make an intent to start Service
                if (!bluetoothOn) {
                    BLEEnable();
                    item.setIcon(R.drawable.ic_bluetoothwait);
                    if (isMyServiceRunning(BLESensor.class)) {
                        Log.i("Home", "Stop services isMyServiceRunning");
                        bleService.setAction(Constants.SERVICE.STOPFOREGROUND_ACTION);
                        stopService(bleService);
                    }
                    Log.i("Home", "Start services");
                    bleService.setAction(Constants.SERVICE.STARTFOREGROUND_ACTION);
                    startForegroundService(bleService);
                    startService(bleService);
                    bluetoothOn = true;
                } else {
                    item.setIcon(R.drawable.ic_bluetoothoff);
                    Log.i("Home", "Stop services");
                    bleService.setAction(Constants.SERVICE.STOPFOREGROUND_ACTION);
                    stopService(bleService);
                    bluetoothOn = false;
                }
                return true;
            case R.id.action_sensor:
                // Make an intent to start next activity.
                Intent i = new Intent(HomeAct.this, SensorData.class);
                startActivity(i);
                return true;
            case R.id.arrow_settings:
                // Make an intent to start next activity.
                Intent j = new Intent(HomeAct.this, SettingsAct.class);
                startActivity(j);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                sensorListener);
        super.onPause();
    }

    @Override

    protected void onStop() {
        super.onStop();

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void BLEEnable() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.BLE.REQUEST_ENABLE_BT);
        }
    }

    private ArrayList<String> splitMessage(String msg) {
        StringTokenizer strTkn = new StringTokenizer(msg, Constants.DECL.DELIMITERS);
        ArrayList<String> arrLis = new ArrayList<>(msg.length());

        while (strTkn.hasMoreTokens())
            arrLis.add(strTkn.nextToken());

        return arrLis;
    }
}
