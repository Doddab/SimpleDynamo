package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;
import android.telephony.TelephonyManager;

import java.io.File;
import java.io.IOException;
//To generate Universally unique identifier
import Helpers.AppData;//Helper classes to perform common functionality
import Helpers.ClientTask;
import Helpers.LinkedList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Timer; //utility class that can be used to schedule a thread to be executed at certain time in future.
import java.util.TimerTask; //is an abstract class that implements Runnable interface and to extend this class
// to create our own TimerTask that can be scheduled using java Timer class.
import java.util.UUID;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import Helpers.LinkedListNode;//Double linked node(easier insert and delete operations)
import Helpers.MySQLHelper;
import Helpers.ServerTask;


public class SimpleDynamoProvider extends ContentProvider {
    MySQLHelper dbh;
    static final int SERVER_PORT = 10000;
    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {

        AppData.buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
        TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        AppData.myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.v(AppData.TAG, "Myport successful");
        AppData.insertResponseMap = new HashMap<String, Boolean>();
        AppData.senderResponseMap = new HashMap<String, String>();
        AppData.senderAllResponseMap = new HashMap<String, String>();
        try {
            /*
			 * Create a server socket and a thread (AsyncTask) that listens on
			 * the server port.
			 */
            ServerSocket ss = new ServerSocket(SERVER_PORT);
            new Thread(new ServerTask(ss)).start();
            Log.v(AppData.TAG, "Successful creation of socket");
        } catch (IOException e) {
            Log.e(AppData.TAG, "Failed creation");
            return false;
        }//Async task creation on server task
        AppData.receiverResponseMap = new HashMap<String, String>();
        AppData.receiverAllResponseMap = new HashMap<String, String>();

		/* Prepare Dynamo List */
        LinkedListNode ln = new LinkedListNode(AppData.REMOTE_PORTS_DYNAMO_ORDER[0], null, null);
        ln.setPreviousNode(ln);
        Log.v(AppData.TAG, "Previous node");
        AppData.dynamoList = new LinkedList(ln);
        ln.setNextNode(ln);
        Log.v(AppData.TAG, "Node and nextnode");
        int i = 1;
        while (i < 5) {
            //for (int i = 1; i < 5; i++) {
            AppData.dynamoList.insertNodeIntoList(AppData.REMOTE_PORTS_DYNAMO_ORDER[i]);
            Log.v(AppData.TAG, "Dynamo chord node created");//Dynamo chord preparation
            i++;
            //}
        }
        Log.v(AppData.TAG, "Setting up the Dynamo node list");
        AppData.keyLockMap = new HashMap<String, Object>();
        AppData.timerMap = new HashMap<UUID, Boolean>();
		/*
		 * Create/retrieve database depending on whether it already exists or
		 * not.
		 */
        final boolean flag1 = dbcheck();
        if (flag1) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    noderecover();
                }
            }, 3000);//utility class that can be used to schedule a thread to be executed at certain time in future.
        }

        if (db != null)
            return true;
        else
            return false;
    }

    public void noderecover() {

        Log.v(AppData.TAG + " Database", "Recovery starting");
        //new Thread(new ClientTask("DeleteAll")).start();
        AppData.myContentResolver.delete(AppData.mUri, "\"@\"", null);
        //AppData.myContentResolver.delete(AppData.mUri, "\"*\"", null);
        Log.v(AppData.TAG + " Database", "Rejoining");
        //AppData.myContentResolver.delete(AppData.mUri, "@", null);
        Cursor csr = AppData.myContentResolver.query(AppData.mUri, null, "\"*\"", null, null);
        //Cursor c = AppData.myContentResolver.query(AppData.mUri, null, "*", null, null);
        if (csr == null) {
            return;
        }
        int kyi = csr.getColumnIndex(AppData.KEY_FIELD);
        int vui = csr.getColumnIndex(AppData.VALUE_FIELD);
        if (kyi == -1 || vui == -1) {
            //Log.e(AppData.TAG, "Wrong columns");
            csr.close();
        }
        //To hash and loop through the dynamo chord to insert the recovered node
        csr.moveToFirst();
        synchronized (AppData.recoveryLock) {
            while (csr.isAfterLast() == false) {
                String k = csr.getString(csr.getColumnIndex(AppData.KEY_FIELD));
                String v = csr.getString(csr.getColumnIndex(AppData.VALUE_FIELD));
                String rm = AppData.dynamoList.getCoordinatorForKey(k);
                String rmnext = AppData.dynamoList.getNextPortNumber(rm);
                String rmnextnext = AppData.dynamoList.getNextPortNumber(rmnext);
                if ((AppData.myPort.equals(rmnextnext)) || (AppData.myPort.equals(rm)) || (AppData.myPort.equals(rmnext))) {
                    if (!(AppData.keyLockMap.keySet().contains(k))) {
                        AppData.keyLockMap.put(k, new Object());
                    }
                    ContentValues cv = new ContentValues();
                    Log.v(AppData.TAG + " Database", "Adding value:" + v);
                    cv.put(AppData.VALUE_FIELD, v);
                    Log.v(AppData.TAG + " Database", "Adding key:" + k);
                    cv.put(AppData.KEY_FIELD, k);
                    //cv.put(AppData.VALUE_FIELD, v);
                    //Insert without conflict and CONFLICT_REPLACE to handle repititive keys across phases
                    long l = db.insertWithOnConflict(AppData.MY_TABLE_NAME, null, cv,
                            SQLiteDatabase.CONFLICT_REPLACE);
                    if (l <= 0) {
                        throw new SQLException("Failed insertion after recovery:" + AppData.mUri);
                    }
                    Log.v(AppData.TAG + " Database", "Replica manager is:" + rm);
                    Log.v(AppData.TAG + " Database", "Next to replica manager is" + rmnext);
                    Log.v(AppData.TAG + " Database", "Next to next to replica manager is" + rmnextnext);
                    Log.v(AppData.TAG + " Failure Recovery", k);
                }
                Log.v(AppData.TAG + "Database", "Failure rec completed");
                csr.moveToNext();
            }
        }//connection closed
        csr.close();
    }

    private boolean dbcheck() {
        boolean flag2 = false;
        try {
            File dbp = this.getContext().getDatabasePath(AppData.DATABASE_NAME);
            if (!dbp.exists()) {
                dbh = new MySQLHelper(getContext());
                db = dbh.getWritableDatabase();
                AppData.mainDatabase = db;
                Log.v(AppData.TAG + "Database", "Creating new database");
            } else {
                Log.v(AppData.TAG + "Database", "Fetching the existing database");
                String abs = dbp.getAbsolutePath();
                SQLiteDatabase retrievedDB = SQLiteDatabase.openDatabase(abs, null, SQLiteDatabase.OPEN_READWRITE);
                db = retrievedDB;
                AppData.mainDatabase = db;
                // new Thread(new ClientTask("DeleteAll")).start();
                flag2 = true;
                //AppData.myContentResolver.delete(AppData.mUri, "\"@\"", null);
                //new Thread(new ClientTask("DeleteAll")).start();
            }
        } catch (SQLiteException e) {

            dbh = new MySQLHelper(getContext());
            db = dbh.getWritableDatabase();
            AppData.mainDatabase = db;
        }
        return flag2;
    }

    private class dynamot extends TimerTask {//is an abstract class that implements Runnable interface and to extend this class
        // to create our own TimerTask that can be scheduled using java Timer class.
        UUID id;

        public dynamot(UUID ID) {
            this.id = ID;
            AppData.timerMap.put(id, true);
        }

        @Override
        public void run() {
            if (AppData.timerMap.containsKey(id)) {
                AppData.timerMap.put(id, false);
            }
            this.cancel();
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        if (!(AppData.keyLockMap.keySet().contains(selection))) {
            AppData.keyLockMap.put(selection, new Object());
        }

        Cursor cursor = null;
        UUID uId = UUID.randomUUID();

        if (selection.equals("\"*\"") || selection.equals("*")) {

			/* Return all key-value pairs from entire Dynamo System */
            new Thread(new ClientTask("QueryAll")).start();

            Timer timer = new Timer();
            timer.schedule(new dynamot(uId), 5000);

            int i = 0;
            int length = AppData.dynamoList.getLength();
            Log.v(AppData.TAG + uId + " Query All", "Waiting for " + length + " responses or 60s!");
            while ((AppData.receivedResponses != (length)) && (AppData.timerMap.get(uId))) {
				/* Wait for response to come */
                if ((i % 10000000) == 0) {
                    Log.v(AppData.TAG + uId, "Provider - 3");
                }
                i++;
            }
            Log.v(AppData.TAG + uId + " Query All", "Got " + AppData.receivedResponses
                    + " responses");

			/* Get back all responses in the receiverAllResponseMap and put them into one cursor. */
            MatrixCursor m = new MatrixCursor(
                    new String[]{AppData.KEY_FIELD, AppData.VALUE_FIELD});
            for (String s : AppData.receiverAllResponseMap.keySet()) {
                m.addRow(new String[]{s, AppData.receiverAllResponseMap.get(s)});
            }
            cursor = m;
            m.close();

            AppData.receiverAllResponseMap.clear();
            AppData.receivedResponses = 0;
            AppData.queryAllTimeoutOccurred = false;

        } else if (selection.equals("\"@\"") || selection.equals("@")) {
			/*
			 * Received a query @ operation. Return key-value pairs from only
			 * this node.
			 */
            cursor = db.rawQuery("SELECT " + AppData.KEY_FIELD + ", " + AppData.VALUE_FIELD
                    + " FROM " + AppData.MY_TABLE_NAME, null);
        } else {

            String coordinator = AppData.dynamoList.getCoordinatorForKey(selection);
            String nextPortNumber = AppData.dynamoList.getNextPortNumber(coordinator);
            String nextToNextPortNumber = AppData.dynamoList.getNextPortNumber(nextPortNumber);

            if ((selectionArgs != null) && (selectionArgs.length > 0)
                    && (selectionArgs[0].equals("SimplyQuery"))) {
                synchronized (AppData.keyLockMap.get(selection)) {
                    synchronized (AppData.recoveryLock) {
                        cursor = db.rawQuery("SELECT " + AppData.KEY_FIELD + ", "
                                + AppData.VALUE_FIELD + " FROM " + AppData.MY_TABLE_NAME
                                + " WHERE key = ?", new String[]{selection});
                    }
                }
            } else { /* Main query handling. */

                if (coordinator.equals(AppData.myPort)) {
                    synchronized (AppData.keyLockMap.get(selection)) {
                        synchronized (AppData.recoveryLock) {
                            cursor = db.rawQuery("SELECT " + AppData.KEY_FIELD + ", "
                                    + AppData.VALUE_FIELD + " FROM " + AppData.MY_TABLE_NAME
                                    + " WHERE key = ?", new String[]{selection});
                        }
                    }
                } else {

                    while (AppData.receiverResponseMap.get(selection) != null) ;
                    synchronized (AppData.receiverResponseMap) {
                        AppData.receiverResponseMap.put(selection, "");
                    }

					/* Send query to the coordinator */
                    new Thread(new ClientTask("QueryFromCoordinator", selection, coordinator,
                            AppData.myPort)).start();
                    Log.v(AppData.TAG + uId, "Sent query " + selection + " to " + coordinator);

                    Timer timer = new Timer();
                    timer.schedule(new dynamot(uId), 3500);

                    int i = 0;
                    while ((AppData.receiverResponseMap.get(selection).equals(""))
                            && (AppData.timerMap.get(uId))) {
                        if ((i % 10000000) == 0) {
                            Log.v(AppData.TAG + uId, "Provider - 4.ii.a " + selection + " : "
                                    + AppData.receiverResponseMap.get(selection) + "%%");
                        }
                        i++;
                    }

					/* If timeout, query middle of the chain */
                    if (AppData.receiverResponseMap.get(selection).equals("")) {
                        timer = new Timer();
                        timer.schedule(new dynamot(uId), 3500);

						/* Query the middle of the chain */
                        new Thread(new ClientTask("QueryFromCoordinator", selection,
                                nextPortNumber, AppData.myPort)).start();
                        Log.v(AppData.TAG + uId, "Sent next query " + selection + " to "
                                + nextPortNumber);

                        i = 0;
                        while ((AppData.receiverResponseMap.get(selection).equals(""))
                                && (AppData.timerMap.get(uId))) {
                            if ((i % 10000000) == 0) {
                                Log.v(AppData.TAG + uId, "Provider - 4.ii.b " + selection + " : "
                                        + AppData.receiverResponseMap.get(selection) + "%%");
                            }
                            i++;
                        }
                    }

					/* If timeout, query tail of the chain */
                    if (AppData.receiverResponseMap.get(selection).equals("")) {
                        timer = new Timer();
                        timer.schedule(new dynamot(uId), 3500);

						/* Query the tail of the chain */
                        new Thread(new ClientTask("QueryFromCoordinator", selection,
                                nextToNextPortNumber, AppData.myPort)).start();
                        Log.v(AppData.TAG + uId, "Sent final query " + selection + " to "
                                + nextToNextPortNumber);

                        i = 0;
                        while ((AppData.receiverResponseMap.get(selection).equals(""))
                                && (AppData.timerMap.get(uId))) {
                            if ((i % 10000000) == 0) {
                                Log.v(AppData.TAG + uId, "Provider - 4.ii.c " + selection + " : "
                                        + AppData.receiverResponseMap.get(selection) + "%%");
                            }
                            i++;
                        }
                    }

                    Log.v(AppData.TAG + uId + uId, "Got query response for " + selection);

                    MatrixCursor m = new MatrixCursor(new String[]{AppData.KEY_FIELD,
                            AppData.VALUE_FIELD});
                    m.addRow(new String[]{selection, AppData.receiverResponseMap.get(selection)});
                    cursor = m;
                    m.close();

                    synchronized (AppData.receiverResponseMap) {
                        AppData.receiverResponseMap.remove(selection);
                    }
                }
            }
        }

        if (AppData.timerMap.containsKey(uId)) {
            AppData.timerMap.remove(uId);
        }

        Log.v("query", selection + " on : " + uId);

        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        if (selection.equals("\"*\"") || selection.equals("*")) {
			/* Delete all key-value pairs from entire Dynamo */
            new Thread(new ClientTask("DeleteAll")).start();
        } else if (selection.equals("\"@\"") || selection.equals("@")) {
			/*
			 * Received a delete @ operation. Delete key-value pairs from only
			 * this node
			 */
            db.delete(AppData.MY_TABLE_NAME, null, null);
        } else { /* Delete based on selection parameter */

            String coordinator = AppData.dynamoList.getCoordinatorForKey(selection);
            String nextPortNumber = AppData.dynamoList.getNextPortNumber(coordinator);
            String nextToNextPortNumber = AppData.dynamoList.getNextPortNumber(nextPortNumber);

            new Thread(new ClientTask("Delete", selection, nextPortNumber)).start();
            new Thread(new ClientTask("Delete", selection, nextToNextPortNumber)).start();

			/* Local delete */
            if (coordinator.equals(AppData.myPort)) {
                Log.v(AppData.TAG + " Deleting", selection);
                db.delete(AppData.MY_TABLE_NAME,
                        AppData.KEY_FIELD + "='" + selection + "'", null);
            } else {
				/* Send out delete message to the correct node */
                new Thread(new ClientTask("Delete", selection, coordinator)).start();
            }
        }
        return 0;
    }

    public Uri insert(Uri uri, ContentValues values) {

        String v2 = (String) values.get(AppData.VALUE_FIELD);
        Log.v(AppData.TAG + " Database", "Inserted value:" + v2);
        String k2 = (String) values.get(AppData.KEY_FIELD);
        Log.v(AppData.TAG + " Database", "Inserted key:" + k2);
        Integer intn = values.getAsInteger(AppData.INSERT_FIELD);
        Log.v(AppData.TAG + " Database", "Integer equivalent:" + intn);
        String ismgr = AppData.dynamoList.getCoordinatorForKey(k2);
        Log.v(AppData.TAG + " Database", "Replica manager is:" + ismgr);
        String ismgrnext = AppData.dynamoList.getNextPortNumber(ismgr);
        Log.v(AppData.TAG + " Database", "Next to replica manager is:" + ismgrnext);
        String ismgrnextnext = AppData.dynamoList.getNextPortNumber(ismgrnext);
        Log.v(AppData.TAG + " Database", "Next to next to replica manager is:" + ismgrnextnext);
        boolean flag3 = ((intn != null) && (intn
                .intValue() == 1));
        Log.v(AppData.TAG + " Database", "indicating inserted node" + flag3);
        boolean reps = ((AppData.myPort.equals(ismgrnext)) || (AppData.myPort
                .equals(ismgrnextnext)));
        Log.v(AppData.TAG + " Database", "node replicas:" + reps);
        boolean rmcheck = (AppData.myPort.equals(ismgr));
        //Log.v(AppData.TAG + " Database", "Key in insert is:" + key);


        if (!(AppData.keyLockMap.keySet().contains(k2))) {
            AppData.keyLockMap.put(k2, new Object());
        }

		/*
		 * Get correct node to insert into from linked list based on key's hash
		 * value
		 */
        if ((!flag3)) {
            //new Thread(new ClientTask("DeleteAll")).start();
            if (!rmcheck) {
                Log.v(AppData.TAG + " Sending Insert", k2 + " --> " + ismgr);
                new Thread(new ClientTask("SimplyInsert", k2, v2, ismgr)).start();
            }
            new Thread(new ClientTask("SimplyInsert", k2, v2, ismgrnext)).start();
            new Thread(new ClientTask("SimplyInsert", k2, v2, ismgrnextnext)).start();
            Log.v(AppData.TAG + " Sending Replicate", k2 + " to " + ismgrnext + " and "
                    + ismgrnextnext);
        }

        if ((flag3 && (reps || rmcheck)) || (rmcheck)) {
            synchronized (AppData.recoveryLock) {
                synchronized (AppData.keyLockMap.get(k2)) {
					/* Replication */
                    long l2 = db.insertWithOnConflict(AppData.MY_TABLE_NAME, null, values,
                            SQLiteDatabase.CONFLICT_REPLACE);
                    if (l2 <= 0) {
                        throw new SQLException("Failed to add a new record into " + uri);
                    }
                    Log.v(AppData.TAG + " Inserting", k2);
                }
            }
        }

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}

