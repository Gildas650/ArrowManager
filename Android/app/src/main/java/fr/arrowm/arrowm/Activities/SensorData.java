package fr.arrowm.arrowm.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import fr.arrowm.arrowm.R;

public class SensorData extends AppCompatActivity {


    public static final String PREFERENCES = "ArrowPrefs";
    public static final String SENSOR_NAME = "sensorName";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_COUNT = "sensorCount";
    public static final String SENSOR_TIME = "sensorTime";
    public static final String SENSOR_POWER = "sensorPower";
    private TextView arrow_count;
    private TextView arrow_millis;
    private TextView arrow_power;
    private EditText sName;
    private SharedPreferences sharedpreferences;
    private SharedPreferences.Editor editor;
    private Integer count = 0;
    private BroadcastReceiver sensorListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(SENSOR_COUNT, -1) != -1) {
                arrow_count.setText(intent.getIntExtra(SENSOR_COUNT, -1) + "");
            }
            if (intent.getIntExtra(SENSOR_TIME, -1) != -1) {
                Float t = (float) intent.getIntExtra(SENSOR_TIME, -1) / 1000;
                arrow_millis.setText(t + "");
            }
            if (intent.getFloatExtra(SENSOR_POWER, -1.0F) != -1) {
                arrow_power.setText(intent.getFloatExtra(SENSOR_POWER, -1.0F) + "");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_data);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();

        arrow_count = (TextView) findViewById(R.id.arrow_count);
        arrow_millis = (TextView) findViewById(R.id.arrow_millis);
        arrow_power = (TextView) findViewById(R.id.arrow_power);
        sName = (EditText) findViewById(R.id.SensorName);
        sName.setText(sharedpreferences.getString(SENSOR_NAME, ""));

        //updateView uv = new updateView();
        //uv.execute();

        final Button ret = (Button) findViewById(R.id.returnb);
        ret.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                editor.putString(SENSOR_NAME, sName.getText().toString());
                editor.commit();

                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                sensorListener);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorListener,
                new IntentFilter(SENSOR));
    }


}
