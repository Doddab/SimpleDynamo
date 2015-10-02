package Helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Formatter;
import java.util.HashSet;
import java.util.UUID;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class AppData {

    public static final String TAG = "XXX ";

    public static final String DATABASE_NAME = "mydsdatabase";
    public static final String MY_TABLE_NAME = "mydstable";
    public static final String KEY_FIELD = "key";
    public static final String VALUE_FIELD = "value";
    public static final String INSERT_FIELD = "insertval";

    public static final String REMOTE_PORTS[] = {"11108", "11112", "11116", "11120", "11124"};
    public static final String REMOTE_PORTS_DYNAMO_ORDER[] = {"11108", "11116", "11120", "11124", "11112"};

    /* This is the port X 2 version, i.e, 11108 - 11124 */
    public static String myPort;

    public static Uri mUri = null;
    public static ContentResolver myContentResolver;
    public static SQLiteDatabase mainDatabase;

    public static LinkedList dynamoList;
    public static boolean allNodesJoined;

    public static HashSet<String> keysBeingHandled = new HashSet<String>();
    public static ArrayList<String> keys = new ArrayList<String>();

    public static HashMap<String, Object> keyLockMap;
    public static HashMap<UUID, Boolean> timerMap;

    public static int receivedResponses = 0;
    public static boolean insertTimeoutOccurred;
    public static boolean queryAllTimeoutOccurred;

    public static Object recoveryLock = new Object();

    public static HashMap<String, Boolean> insertResponseMap;
    public static HashMap<String, String> senderResponseMap;
    public static HashMap<String, String> senderAllResponseMap;
    public static HashMap<String, String> receiverResponseMap;
    public static HashMap<String, String> receiverAllResponseMap;

    public static void buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        mUri = uriBuilder.build();
    }

    @SuppressWarnings("resource")
    public static String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

}
