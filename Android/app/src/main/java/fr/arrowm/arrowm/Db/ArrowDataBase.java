package fr.arrowm.arrowm.Db;

/**
 * Created by Gildas on 10/12/2016.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import fr.arrowm.arrowm.Business.Event;
import fr.arrowm.arrowm.Business.Impact;
import fr.arrowm.arrowm.Business.Round;
import fr.arrowm.arrowm.Business.Score;
import fr.arrowm.arrowm.Business.Session;

public class ArrowDataBase {
    private static final int DATABASE_VERSION = 6;

    //Sensor Table description : store sensor data
    private static final String DBNAME = "arrow.db";
    private static final String ID = "id";


    //Session Table description : store session data (Training or Competition)
    private static final String SESSION_TABLE_NAME = "session";
    private static final String SESSION_TABLE_NAME_TMP = "session_tmp";
    private static final String NUMBER_OF_ARROWS = "numberofarrows";
    private static final String SESSION_START = "sessionstart";
    private static final String SESSION_END = "sessionend";

    //Score table description : store all score relative to a session (SESSION ID)
    private static final String SCORE_TABLE_NAME = "score";
    private static final String SCORE_TABLE_NAME_TMP = "score_tmp";
    private static final String SESSION_ID = "sessionid";
    private static final String EVENT = "event";
    private static final String SCORE = "score";
    private static final String POS_X = "posx";
    private static final String POS_Y = "posy";

    //Timing table description : store all timing relative to a session (SESSION ID)
    private static final String ARROWTIME_TABLE_NAME = "arrowtime";
    private static final String ARROWTIME_TABLE_NAME_TMP = "arrowtime_tmp";
    private static final String ARROWTIME = "arrowtime";

    private SQLiteDatabase bdd;
    private ArrowDBOpenHelper ArrowBaseSQLite;

    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");

    public ArrowDataBase(Context context) {
        ArrowBaseSQLite = new ArrowDBOpenHelper(context, DBNAME, null, DATABASE_VERSION);
    }

    public void open() {
        bdd = ArrowBaseSQLite.getWritableDatabase();
    }

    public void close() {
        bdd.close();
    }

    public long insert(Session s, Boolean isTemp) {

        //Insert session and retreive Id for all score
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(NUMBER_OF_ARROWS, s.getNumberOfArrows());
        sessionValues.put(SESSION_START, df.format(s.getBeginOfSession()));
        if (s.getEndOfSession() != null) {
            sessionValues.put(SESSION_END, df.format(s.getEndOfSession()));
        }
        long id;
        if(isTemp) {
            id = bdd.insert(SESSION_TABLE_NAME_TMP, null, sessionValues);
        }
        else{
            id = bdd.insert(SESSION_TABLE_NAME, null, sessionValues);
        }

        //Insert scores
        if (s.isExistRound()) {
            Round r = s.getRound();
            ContentValues roundValues = new ContentValues();
            for (int i = 0; i < r.getScorecard().size(); i++) {
                roundValues.put(SESSION_ID, id);
                roundValues.put(EVENT, r.getEvent().getId());
                roundValues.put(SCORE, r.getScorecard().get(i).getId());
                roundValues.put(POS_X, r.getImpactcard().get(i).getX());
                roundValues.put(POS_Y, r.getImpactcard().get(i).getY());
                if(isTemp) {
                    bdd.insert(SCORE_TABLE_NAME_TMP, null, roundValues);
                }
                else{
                    bdd.insert(SCORE_TABLE_NAME, null, roundValues);
                }
            }
        }

        //Insert timings
        if (s.getArrowTime().size() > 0) {
            ContentValues timingValues = new ContentValues();
            for (int j = 0; j < s.getArrowTime().size(); j++) {
                timingValues.put(SESSION_ID, id);
                timingValues.put(ARROWTIME, s.getArrowTime().get(j));
                if(isTemp){
                    bdd.insert(ARROWTIME_TABLE_NAME_TMP, null, timingValues);
                }
                else {
                    bdd.insert(ARROWTIME_TABLE_NAME, null, timingValues);
                }
            }
        }

        return id;
    }

    //Remove a session and all relative scores
    public int removeSession(int id) {
        String[] args = {id + ""};
        Cursor cursor = bdd.rawQuery("SELECT * from " + SCORE_TABLE_NAME + " WHERE " + SESSION_ID + " = ? ", args);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            bdd.delete(SCORE_TABLE_NAME, ID + " = " + cursor.getInt(0), null);
            bdd.delete(ARROWTIME_TABLE_NAME, ID + " = " + cursor.getInt(0), null);
        }
        cursor.close();
        return bdd.delete(SESSION_TABLE_NAME, ID + " = " + id, null);
    }

    //Remove a session and all relative scores
    public void dropTmp() {
        bdd.execSQL("delete from "+ ARROWTIME_TABLE_NAME_TMP);
        bdd.execSQL("delete from "+ SCORE_TABLE_NAME_TMP);
        bdd.execSQL("delete from "+ SESSION_TABLE_NAME_TMP);
    }

    //Fecth a session object from his ID in DB
    private Session selectSessionWithID(String[] args, Boolean isTemp) {
        Session s;
        if(isTemp){
            Cursor sessionCursor = bdd.rawQuery("SELECT * from " + SESSION_TABLE_NAME_TMP + " WHERE id = ? ", args);
            Cursor roundCursor = bdd.rawQuery("SELECT * from " + SCORE_TABLE_NAME_TMP + " WHERE " + SESSION_ID + " = ? ", args);
            Cursor timingCursor = bdd.rawQuery("SELECT * from " + ARROWTIME_TABLE_NAME_TMP + " WHERE " + SESSION_ID + " = ? ", args);
            s = cursorToSession(sessionCursor, roundCursor, timingCursor);
            sessionCursor.close();
            roundCursor.close();
            timingCursor.close();
        }
        else {
            Cursor sessionCursor = bdd.rawQuery("SELECT * from " + SESSION_TABLE_NAME + " WHERE id = ? ", args);
            Cursor roundCursor = bdd.rawQuery("SELECT * from " + SCORE_TABLE_NAME + " WHERE " + SESSION_ID + " = ? ", args);
            Cursor timingCursor = bdd.rawQuery("SELECT * from " + ARROWTIME_TABLE_NAME + " WHERE " + SESSION_ID + " = ? ", args);
            s = cursorToSession(sessionCursor, roundCursor, timingCursor);
            sessionCursor.close();
            roundCursor.close();
            timingCursor.close();
        }
        return s;
    }

    //Select all session in DB (used for list of sessions)
    public LinkedList<Session> selectAll() {
        LinkedList<Session> sessions = new LinkedList<>();
        Cursor cursor = bdd.rawQuery("SELECT " + ID + " from " + SESSION_TABLE_NAME, null);
        Session session;
        if (cursor.getCount() > 0) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String[] tmp = {cursor.getInt(0) + ""};
                session = selectSessionWithID(tmp,false);
                sessions.add(0, session);
            }
        }
        cursor.close();
        return sessions;
    }

    //Select last session in DB (used for list of sessions)
    public Session selectTmp() {
        Cursor cursor = bdd.rawQuery("SELECT "+ ID + " from " + SESSION_TABLE_NAME_TMP , null);
        Session session = null;
        if (cursor.getCount() > 0) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String[] tmp = {cursor.getInt(0) + ""};
                session = selectSessionWithID(tmp, true);
            }
        }
        cursor.close();
        return session;
    }

    //Fect information for objective progress bar in Home Activity (index 0 : number of arrow in next week, index 1 : number of arrows in last month)
    public ArrayList<String> defineObjResult() {
        ArrayList<String> ret = new ArrayList<>();
        Calendar ref = Calendar.getInstance();
        ref.add(Calendar.DATE, +1);
        Calendar week = Calendar.getInstance();
        week.add(Calendar.DATE, -7);
        Cursor cursor = bdd.rawQuery("SELECT sum(" + NUMBER_OF_ARROWS + ") from " + SESSION_TABLE_NAME + " WHERE " + SESSION_END + " > '" + df2.format(week.getTime()) + "'", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            ret.add(cursor.getString(0));
        }
        cursor.close();
        Calendar month = Calendar.getInstance();
        month.add(Calendar.MONTH, -1);
        Cursor cursor2 = bdd.rawQuery("SELECT sum(" + NUMBER_OF_ARROWS + ") from " + SESSION_TABLE_NAME + " WHERE " + SESSION_END + " > '" + df2.format(month.getTime()) + "'", null);
        if (cursor2.getCount() > 0) {
            cursor2.moveToFirst();
            ret.add(cursor2.getString(0));
        }
        cursor2.close();
        return ret;
    }

    //Transform Cursor in Session object with relative Score in a Round object
    private Session cursorToSession(Cursor sc, Cursor rc, Cursor tc) {
        sc.moveToFirst();
        Session s = new Session();
        s.setDbId(sc.getInt(0));
        s.setNumberOfArrows(sc.getInt(1));
        s.setBeginOfSession(textToDate(sc.getString(2)));
        if (sc.getString(3) != null) {
            s.setEndOfSession(textToDate(sc.getString(3)));
        }
        if (rc != null) {
            if (rc.getCount() > 0) {
                rc.moveToFirst();
                Event e = Event.values()[rc.getInt(2)];
                Round r = new Round(e);
                do {
                    Score score = Score.values()[rc.getInt(3)];
                    r.getScorecard().add(score);
                    r.getImpactcard().add(new Impact(rc.getFloat(4), rc.getFloat(5)));
                } while (rc.moveToNext());
                s.setRound(r);
            }
        }
        if (tc != null) {
            if (tc.getCount() > 0) {
                tc.moveToFirst();
                do {
                    s.addArrowTime(tc.getInt(2));
                } while (tc.moveToNext());
            }
        }
        return s;
    }

    //Transform a int into a boolean
    private boolean intToBoolean(int transform) {
        boolean ret = false;
        if (transform == 1) {
            ret = true;
        }
        return ret;
    }

    //Transform a text into a date
    private Date textToDate(String transform) {
        Date ret = new Date();
        try {
            if (transform != null) {
                ret = df.parse(transform);
            }
            else{
                ret = null;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return ret;
    }
}