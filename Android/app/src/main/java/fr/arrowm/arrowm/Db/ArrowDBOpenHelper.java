package fr.arrowm.arrowm.Db;

/**
 * Created by Gildas on 10/12/2016.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class ArrowDBOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 5;

    private static final String SENSOR_TABLE_NAME = "sensor";
    /*private static final String ID = "id";
    private static final String RAW_1 = "raw1";
    private static final String RAW_2 = "raw2";
    private static final String RAW_3 = "raw3";
    private static final String ARROW_1 = "arrow1";
    private static final String ARROW_2 = "arrow2";
    private static final String ARROW_3 = "arrow3";*/

    private static final String ID = "id";
    private static final String SESSION_ID = "sessionid";
    private static final String SESSION_INDEX = "sessionindex";
    private static final String SESSION_INDEX_TIME = "sessionindextime";

    private static final String SESSION_TABLE_NAME = "session";
    private static final String NUMBER_OF_ARROWS = "numberofarrows";
    private static final String SESSION_START = "sessionstart";
    private static final String SESSION_END = "sessionend";
    private static final String IS_COMPETITION = "iscompetition";
    private static final String COMMENT = "comment";

    private static final String SCORE_TABLE_NAME = "score";
    private static final String EVENT = "event";
    private static final String SCORE = "score";
    private static final String POS_X = "posx";
    private static final String POS_Y = "posy";

    private static final String ARROWTIME_TABLE_NAME = "arrowtime";
    private static final String ARROWTIME = "arrowtime";


    /*private static final String SENSOR_TABLE_CREATE =
            "CREATE TABLE " + SENSOR_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY, "
                    + RAW_1 + " FLOAT DEFAULT 0, " + RAW_2 + " FLOAT DEFAULT 0, " + RAW_3 + " FLOAT DEFAULT 0, "
                    + ARROW_1 + " INTEGER DEFAULT 0, " + ARROW_2 + " INTEGER DEFAULT 0, " + ARROW_3 + " INTEGER DEFAULT 0);";

    private static final String SENSOR_TABLE_INIT =
            "INSERT INTO " + SENSOR_TABLE_NAME + " (" + ID + ") VALUES (1);";*/

    private static final String SESSION_TABLE_CREATE =
            "CREATE TABLE " + SESSION_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + NUMBER_OF_ARROWS + " INTEGER DEFAULT 0, " + SESSION_START + " TEXT, " + SESSION_END + " TEXT, "
                    + IS_COMPETITION + " INTEGER DEFAULT 0, " + COMMENT + " TEXT);";

    private static final String SCORE_TABLE_CREATE =
            "CREATE TABLE " + SCORE_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + SESSION_ID + " INTEGER, " + EVENT + " INTEGER , " + SCORE + " INTEGER, "
                    + POS_X + " FLOAT DEFAULT 0 , " + POS_Y + " FLOAT DEFAULT 0);";

    private static final String SCORE_TABLE_INDEX_CREATE =
            "CREATE INDEX " + SESSION_INDEX + " ON " + SCORE_TABLE_NAME + " (" + SESSION_ID + ");";

    private static final String ARROWTIME_TABLE_CREATE =
            "CREATE TABLE " + ARROWTIME_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + SESSION_ID + " INTEGER, " + ARROWTIME + " INTEGER);";

    private static final String ARROWTIME_TABLE_INDEX_CREATE =
            "CREATE INDEX " + SESSION_INDEX_TIME + " ON " + ARROWTIME_TABLE_NAME + " (" + SESSION_ID + ");";


    public ArrowDBOpenHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /*
        db.execSQL(SENSOR_TABLE_CREATE);
        db.execSQL(SENSOR_TABLE_INIT);*/
        db.execSQL(SESSION_TABLE_CREATE);
        db.execSQL(SCORE_TABLE_CREATE);
        db.execSQL(SCORE_TABLE_INDEX_CREATE);
        db.execSQL(ARROWTIME_TABLE_CREATE);
        db.execSQL(ARROWTIME_TABLE_INDEX_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + SENSOR_TABLE_NAME + ";");
        db.execSQL("DROP TABLE IF EXISTS " + SESSION_TABLE_NAME + ";");
        db.execSQL("DROP TABLE IF EXISTS " + SCORE_TABLE_NAME + ";");
        db.execSQL("DROP TABLE IF EXISTS " + ARROWTIME_TABLE_NAME + ";");
        onCreate(db);
    }
}
