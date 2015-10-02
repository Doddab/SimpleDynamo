
package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.Timer;
import java.util.TimerTask;

import Helpers.AppData;
import Helpers.LinkedListNode;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class SimpleDynamoActivity extends Activity {

    public static TextView mtv;

    static int counter = 0;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dynamo);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        mtv = tv;

        /* Keep a copy of the content resolver accessible from everywhere */
        AppData.myContentResolver = getContentResolver();

        findViewById(R.id.insertBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int i = counter;
                counter += 1;
                for (; i < counter; i++) {
                    ContentValues cv = new ContentValues();
                    cv = new ContentValues();
                    cv.put(AppData.KEY_FIELD, "key" + Integer.toString(i));
                    cv.put(AppData.VALUE_FIELD, "val" + Integer.toString(i));
                    getContentResolver().insert(AppData.mUri, cv);
                    mtv.append("Inserting  key " + i + " --> val " + i + "\n");
                }
            }
        });

		/* LDump logic */
        findViewById(R.id.ldumpBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Cursor resultCursor = getContentResolver().query(AppData.mUri, null, "\"@\"", null,
                //null);
                Cursor resultCursor = getContentResolver().query(AppData.mUri, null, "\"@\"", null,
                        null);
                printValues(resultCursor);
            }
        });

		/* GDump logic */
        findViewById(R.id.gdumpBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Cursor resultCursor = getContentResolver().query(AppData.mUri, null, "\"*\"", null,
                        null);
                printValues(resultCursor);
            }
        });

		/* LDelete logic */
        findViewById(R.id.ldeleteBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getContentResolver().delete(AppData.mUri, "\"@\"", null);
            }
        });

		/* GDelete logic */
        findViewById(R.id.gdeleteBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getContentResolver().delete(AppData.mUri, "\"*\"", null);
            }
        });

		/* Clear logic */
        findViewById(R.id.clearBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mtv.setText("");
            }
        });

        Timer t = new Timer();
        t.schedule(new TimerTask() {

            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        printAfterWait();
                    }
                });
            }
        }, 5000);
    }

    public void printAfterWait() {
        if (AppData.dynamoList != null) {
            LinkedListNode node = AppData.dynamoList.getNodeForPortNumber(AppData.REMOTE_PORTS[0]);
            LinkedListNode newNode = node;
            do {
                mtv.append(String.valueOf(Integer.valueOf(newNode.getPortNumber()) / 2) + "   ");
                newNode = newNode.getNextNode();
            } while (newNode != node);
        }
    }

    public void printValues(Cursor resultCursor) {

        try {
            if (resultCursor == null) {
                Log.e(AppData.TAG, "Result null");
                throw new Exception();
            }

            int keyIndex = resultCursor.getColumnIndex(AppData.KEY_FIELD);
            int valueIndex = resultCursor.getColumnIndex(AppData.VALUE_FIELD);
            if (keyIndex == -1 || valueIndex == -1) {
                Log.e(AppData.TAG, "Wrong columns");
                resultCursor.close();
                throw new Exception();
            }

            resultCursor.moveToFirst();

            while (resultCursor.isAfterLast() == false) {
                String returnKey = resultCursor.getString(keyIndex);
                String returnValue = resultCursor.getString(valueIndex);
                mtv.append("\n" + returnKey + " : " + returnValue);
                resultCursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.simple_dynamo, menu);
        return true;
    }

}
