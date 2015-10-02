package Helpers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import android.util.Log;

/**
 * ClientTask is a runnable that should send a string over the network. It is
 * created by constructor call whenever required.
 */
public class ClientTask implements Runnable {

    String msgs[];
    int queryAllFailed;

    public ClientTask(String... messages) {
        this.msgs = messages;
    }

    @Override
    public void run() {

        try {

            if ((msgs[0].equals("DeleteAll")) || (msgs[0].equals("QueryAll"))) {

                String remotePort = null;
                MessageHolder mHolder = null;

                if (msgs[0].equals("DeleteAll")) {
                    mHolder = new MessageHolder(msgs[0]);
                } else if (msgs[0].equals("QueryAll")) {
                    mHolder = new MessageHolder(msgs[0], null, AppData.myPort);
                }

                try {
                    for (int i = 0; i < 5; i++) {
                        queryAllFailed = i;
                        remotePort = AppData.REMOTE_PORTS[i];

                        Log.v(AppData.TAG + " Client Sending", mHolder.message + " to "
                                + AppData.REMOTE_PORTS[i]);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2,
                                2}), Integer.parseInt(remotePort));

                        OutputStream outStream = socket.getOutputStream();
                        ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
                        objOutStream.writeObject(mHolder);
                        objOutStream.close();
                        outStream.close();
                        socket.close();
                    }
                } catch (IOException e) {
                    int i = queryAllFailed + 1;
                    for (; i < 5; i++) {
                        remotePort = AppData.REMOTE_PORTS[i];

                        Log.v(AppData.TAG + " Client Sending", mHolder.message + " to "
                                + AppData.REMOTE_PORTS[i]);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2,
                                2}), Integer.parseInt(remotePort));

                        OutputStream outStream = socket.getOutputStream();
                        ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
                        objOutStream.writeObject(mHolder);
                        objOutStream.close();
                        outStream.close();
                        socket.close();
                    }
                }
            } else {

                String remotePort = null;
                MessageHolder mHolder = null;

                if (msgs[0].equals("SimplyInsert")) {
                    /* Send to the required node */
                    mHolder = new MessageHolder(msgs[0], msgs[1], msgs[2], AppData.myPort);
                    remotePort = msgs[3];
                } else if (msgs[0].equals("QueryFromCoordinator")) {
					/* Sending directly to the coordinator/tail of the key */
                    mHolder = new MessageHolder(msgs[0], msgs[1], msgs[3]);
                    remotePort = msgs[2];
                } else if (msgs[0].equals("QueryResponse")) {

                    remotePort = msgs[1];

					/* Send the response to the correct node */
                    if (msgs[2].equals("All")) {
                        mHolder = new MessageHolder(msgs[0], AppData.senderAllResponseMap, true);
                        AppData.senderAllResponseMap = new HashMap<String, String>();
                        Log.v(AppData.TAG, "Sending query all response to " + remotePort);
                    } else {
                        HashMap<String, String> result = new HashMap<String, String>();
                        result.put(msgs[3], msgs[4]);
                        mHolder = new MessageHolder(msgs[0], result, false);
                        Log.v(AppData.TAG, "Sending query response to " + remotePort);
                    }
                } else if ((msgs[0].equals("Delete")) || (msgs[0].equals("DeleteReplica"))) {
					/* Send to the correct node */
                    mHolder = new MessageHolder(msgs[0], msgs[1], null);
                    remotePort = msgs[2];
                }

                Log.v(AppData.TAG + " Client sending", mHolder.message + "");

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                OutputStream outStream = socket.getOutputStream();
                ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
                objOutStream.writeObject(mHolder);
                objOutStream.close();
                outStream.close();
                socket.close();
            }
        } catch (UnknownHostException e) {
            Log.e(AppData.TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(AppData.TAG, "ClientTask socket IOException");
        }
    }
}
