package fr.arrowm.arrowm.Activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import java.util.List;

import fr.arrowm.arrowm.Adapters.SessionAdapter;
import fr.arrowm.arrowm.Business.Session;
import fr.arrowm.arrowm.Db.ArrowDataBase;
import fr.arrowm.arrowm.R;

public class LogAct extends AppCompatActivity {

    private ListView logView;
    private ArrowDataBase db = new ArrowDataBase(this);
    private List<Session> sessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        logView = (ListView) findViewById(R.id.logView);
        logView.setItemsCanFocus(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        db.open();
        this.sessions = db.selectAll();
        db.close();

        for (int i =0; i<this.sessions.size(); i++){
            if (this.sessions.get(i).getEndOfSession() == null){
                this.sessions.remove(i);
                i = i-1;
            }
        }
        SessionAdapter adapter = new SessionAdapter(LogAct.this, sessions);
        logView.setAdapter(adapter);
    }
}