package Helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE " + AppData.MY_TABLE_NAME + " ("
            + AppData.KEY_FIELD + " TEXT PRIMARY KEY, " + AppData.VALUE_FIELD + " TEXT, " + AppData.INSERT_FIELD + " INT);";

    public MySQLHelper(Context context) {
        super(context, AppData.DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v(AppData.MY_TABLE_NAME + "Database", "Creating DB for the first time");
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
    }

}
