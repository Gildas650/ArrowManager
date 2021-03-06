package fr.arrowm.arrowm.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import fr.arrowm.arrowm.Business.Constants;
import fr.arrowm.arrowm.R;

//Setting page
public class SettingsAct extends AppCompatActivity {


    private SharedPreferences.Editor editor;
    private Spinner bows;
    private EditText objbyweek;
    private EditText objbymonth;
    private EditText objbysce;
    private EditText toleranceTiming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        bows = (Spinner) findViewById(R.id.bows);
        objbyweek = (EditText) findViewById(R.id.objbyweek);
        objbymonth = (EditText) findViewById(R.id.objbymonth);
        objbysce = (EditText) findViewById(R.id.objbysce);
        toleranceTiming = (EditText) findViewById(R.id.toleranceTiming);
        SharedPreferences sharedpreferences = getSharedPreferences(Constants.DECL.PREFERENCES, Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        if (sharedpreferences.getString(Constants.DECL.BOW_TYPE, "").equals("Compound")) {
            bows.setSelection(0);
        } else {
            bows.setSelection(1);
        }
        objbysce.setText(sharedpreferences.getString(Constants.DECL.OBJ_SCE, "100"));
        objbyweek.setText(sharedpreferences.getString(Constants.DECL.OBJ_WEEK, "500"));
        objbymonth.setText(sharedpreferences.getString(Constants.DECL.OBJ_MONTH, "2000"));
        toleranceTiming.setText(sharedpreferences.getString(Constants.DECL.TOL_TIMING, "1"));

        final Button ret = (Button) findViewById(R.id.validate);
        ret.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editor.putString(Constants.DECL.BOW_TYPE, String.valueOf(bows.getSelectedItem()));
                editor.putString(Constants.DECL.OBJ_SCE, String.valueOf(objbysce.getText().toString()));
                editor.putString(Constants.DECL.OBJ_WEEK, String.valueOf(objbyweek.getText().toString()));
                editor.putString(Constants.DECL.OBJ_MONTH, String.valueOf(objbymonth.getText().toString()));
                editor.putString(Constants.DECL.TOL_TIMING, String.valueOf(toleranceTiming.getText().toString()));
                editor.commit();
                finish();
            }
        });
    }


}
