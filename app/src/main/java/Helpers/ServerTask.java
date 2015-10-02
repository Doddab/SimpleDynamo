package Helpers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

/***
 * ServerTask is a runnable that should handle incoming messages. It is created
 * by ServerTask.executeOnExecutor() call.
 */
public class ServerTask implements Runnable {

	int noOfJoinRequests = 0;
	ServerSocket serverSocket = null;

	public ServerTask(ServerSocket socket) {
		this.serverSocket = socket;
	}
	
	@Override
	public void run() {

		try {
			while (true) {
				
				/* Wait till content provider is set up */
				while ((AppData.mUri == null) || (AppData.myContentResolver == null));
				
				Socket socket = serverSocket.accept();

				MessageHolder incomingMessage = (MessageHolder) (new ObjectInputStream(
						socket.getInputStream())).readObject();

				Log.v(AppData.TAG, "Incoming Message : " + incomingMessage.message);

				/* Insert Logic */
				if (incomingMessage.message.equals("SimplyInsert")) {

					ContentValues value = new ContentValues();
					value.put(AppData.KEY_FIELD, incomingMessage.key);
					value.put(AppData.VALUE_FIELD, incomingMessage.value);
					value.put(AppData.INSERT_FIELD, 1);
					
					Log.v(AppData.TAG + "Servercallinginsert", incomingMessage.key);
                    AppData.myContentResolver.insert(AppData.mUri, value);
				}

				else if (incomingMessage.message.equals("QueryFromCoordinator")) {
					
					String value = null;
					Log.v(AppData.TAG + "Servercallingquery ", incomingMessage.key);
					Cursor c = AppData.myContentResolver.query(AppData.mUri, null,
							incomingMessage.key, new String[] { "SimplyQuery" }, null);

					if (c != null) {

						int keyIndex = c.getColumnIndex(AppData.KEY_FIELD);
						int valueIndex = c.getColumnIndex(AppData.VALUE_FIELD);
						if (keyIndex == -1 || valueIndex == -1) {
							Log.e(AppData.TAG, "Wrong columns");
							c.close();
							Log.v(AppData.TAG, incomingMessage.key + " has wrong columns");
							continue;
						}

						c.moveToFirst();

						if (!(c.isFirst() && c.isLast())) {
							Log.e(AppData.TAG, "Wrong number of rows");
							c.close();
							Log.v(AppData.TAG, incomingMessage.key + " has wrong number of rows"
									+ c.getCount());
							continue;
						}

						value = c.getString(c.getColumnIndex(AppData.VALUE_FIELD));
						Log.v(AppData.TAG + "Serverqueryresponse", incomingMessage.key + " : "+ value);
					}
					c.close();
					Log.v(AppData.TAG, "Calling query response client from server "
							+ incomingMessage.key);
					new Thread(new ClientTask("QueryResponse", incomingMessage.value, "Single",
							incomingMessage.key, value)).start();
					
				} else if (incomingMessage.message.equals("QueryAll")) {

					Log.v(AppData.TAG, "Received Query All Message");
					Cursor c = AppData.myContentResolver.query(AppData.mUri, null, "@", null, null);

					if (c != null) {

						int keyIndex = c.getColumnIndex(AppData.KEY_FIELD);
						int valueIndex = c.getColumnIndex(AppData.VALUE_FIELD);
						if (keyIndex == -1 || valueIndex == -1) {
							Log.e(AppData.TAG, "Wrong columns");
							c.close();
							continue;
						}

						c.moveToFirst();

						while (c.isAfterLast() == false) {
                            AppData.senderAllResponseMap.put(
									c.getString(c.getColumnIndex(AppData.KEY_FIELD)),
									c.getString(c.getColumnIndex(AppData.VALUE_FIELD)));
							c.moveToNext();
						}
					}
					
					c.close();
					new Thread(new ClientTask("QueryResponse", incomingMessage.value, "All"))
							.start();
				} else if (incomingMessage.message.equals("QueryResponse")) {

					if (incomingMessage.responseForAll) {

						if (AppData.receiverAllResponseMap == null) {
                            AppData.receiverAllResponseMap = new HashMap<String, String>();
						}
						for (String s : incomingMessage.resultMap.keySet()) {
                            AppData.receiverAllResponseMap.put(s, incomingMessage.resultMap.get(s));
						}
                        AppData.receivedResponses++;
						Log.v(AppData.TAG + " Server Response", "QueryAll got " + AppData.receivedResponses + " responses");
						
					} else {
						for (String s : incomingMessage.resultMap.keySet()) {
							String val = incomingMessage.resultMap.get(s);
							Log.v(AppData.TAG + " Server Response", "Inserting " + s + " : " + val
									+ " into response map");
							synchronized (AppData.receiverResponseMap) {
                                AppData.receiverResponseMap.put(s, val);
							}
						}
					}
				}

				else if (incomingMessage.message.equals("Delete")) {
                    AppData.mainDatabase.delete(AppData.MY_TABLE_NAME, AppData.KEY_FIELD + "='"
							+ incomingMessage.key + "'", null);
				} else if (incomingMessage.message.equals("DeleteAll")) {
                    AppData.mainDatabase.delete(AppData.MY_TABLE_NAME, null, null);
				}
				
				Log.v(AppData.TAG, "Incoming Message processed fully!");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
