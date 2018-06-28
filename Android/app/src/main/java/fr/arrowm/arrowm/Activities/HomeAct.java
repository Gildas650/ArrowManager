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

import fr.arrowm.arrowm.Business.SimpleCircularProgressbar;
import fr.arrowm.arrowm.Db.ArrowDataBase;
import fr.arrowm.arrowm.R;
import fr.arrowm.arrowm.Service.BLESensor;


public class HomeAct extends AppCompatActivity {
    public final static String IS_BLUETOOTHON = "com.arrowM.MESSAGE2";
    public static final String PREFERENCES = "ArrowPrefs";
    public static final String OBJ_WEEK = "objWeek";
    public static final String OBJ_MONTH = "objMonth";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_CONNECTED = "sensorConnected";
    public static final String SENSOR_MSG = "sensorMsg";
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
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
            if (intent.getIntExtra(SENSOR_CONNECTED, STATE_DISCONNECTED) == STATE_CONNECTED) {
                bleState = STATE_CONNECTED;
                item.setIcon(R.drawable.ic_bluetooth);
            } else if (intent.getIntExtra(SENSOR_CONNECTED, STATE_DISCONNECTED) == STATE_CONNECTING) {
                bleState = STATE_CONNECTING;
                item.setIcon(R.drawable.ic_bluetoothwait);
            } else {
                bleState = STATE_DISCONNECTED;
                item.setIcon(R.drawable.ic_bluetoothoff);
            }
            if (!intent.getStringExtra(SENSOR_MSG).equals("")) {
                Log.e("msg", intent.getStringExtra(SENSOR_MSG));
                if (getResources().getIdentifier(intent.getStringExtra(SENSOR_MSG), "string", PACKAGE_NAME) != 0) {
                    Toast.makeText(getApplicationContext(), getResources().getIdentifier(intent.getStringExtra(SENSOR_MSG), "string", PACKAGE_NAME), Toast.LENGTH_LONG).show();
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
        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        final ImageButton nTraining = (ImageButton) findViewById(R.id.TrainingButton);
        nTraining.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(HomeAct.this, SessionAct.class);
                i.putExtra(IS_BLUETOOTHON, bleState);
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
                new IntentFilter(SENSOR));

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
        weeklytext.setText(dObj.get(0) + "/" + sharedpreferences.getString(OBJ_WEEK, "500"));
        monthlytext.setText(dObj.get(1) + "/" + sharedpreferences.getString(OBJ_MONTH, "2000"));
        weeklyprogress.setProgress((Float.parseFloat(dObj.get(0)) / Float.parseFloat(sharedpreferences.getString(OBJ_WEEK, "500"))) * 100);
        monthlyprogress.setProgress((Float.parseFloat(dObj.get(1)) / Float.parseFloat(sharedpreferences.getString(OBJ_MONTH, "2000"))) * 100);

        LocalBroadcastManager.getInstance(this).registerReceiver(sensorListener,
                new IntentFilter(SENSOR));
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
                if (bluetoothOn == false) {
                    BLEEnable();
                    item.setIcon(R.drawable.ic_bluetoothwait);
                    if (isMyServiceRunning(BLESensor.class)) {
                        stopService(bleService);
                    }
                    startService(bleService);
                    bluetoothOn = true;
                } else {
                    item.setIcon(R.drawable.ic_bluetoothoff);
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
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /*BLESensor mService = null;

    boolean mServiceConnected = false;

    private ServiceConnection mConn = new ServiceConnection() {

        @Override

        public void onServiceConnected(ComponentName className, IBinder binder) {

            Log.d("BinderActivity", "Connected to service.");

            mService = ((BLESensor.LocalBinder) binder).getService();

            mServiceConnected = true;

        }*/


    /**

     * Connection dropped.

     */

        /*Override

        public void onServiceDisconnected(ComponentName className) {

            Log.d("BinderActivity", "Disconnected from service.");

            mService = null;

            mServiceConnected = false;

        }

    };*/
}
