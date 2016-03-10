package io.cloudboost.webchat;

import io.cloudboost.CloudApp;
import io.cloudboost.CloudException;
import io.cloudboost.CloudObject;
import io.cloudboost.CloudObjectCallback;
import io.cloudboost.CloudQuery;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ChatBubbleActivity extends Activity {
	private ChatArrayAdapter chatArrayAdapter;
	private ListView listView;
	private EditText chatText;
	private Button buttonSend;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// init the cloudapp
		CloudApp.init("bengi", "ailFnQf+xxxxxxxxx==");
		// inflate main view
		setContentView(R.layout.activity_chat);
		// get the sendbutton
		buttonSend = (Button) findViewById(R.id.buttonSend);
		// get the list view that will display the chats
		listView = (ListView) findViewById(R.id.listView1);

		// create an array adapter to supply chats to the listview
		chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(),
				R.layout.activity_chat_singlemessage);

		listView.setAdapter(chatArrayAdapter);

		// get the edittext box where agent will type chats
		chatText = (EditText) findViewById(R.id.chatText);
		// set this editor to listen to enter key press to send message
		chatText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					return sendMessageToClient();
				}
				return false;
			}
		});

		buttonSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendMessageToClient();
			}
		});

		listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		listView.setAdapter(chatArrayAdapter);

		// to scroll the list view to bottom on data change
		chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				super.onChanged();
				listView.setSelection(chatArrayAdapter.getCount() - 1);
			}
		});
		/*
		 * create a query on table chat, remember we use notification queries
		 * for this example, when u send a chat, we just save it as a
		 * CloudObject in the chat table which the chat widget listens to.
		 */
		CloudQuery query = new CloudQuery("chat");
		/*
		 * we also listen to "created" events on table chat, difference is we
		 * query records where admin column is set to false, that means we shall
		 * not be receiving echoes of our own messages The chat widget on the
		 * site has to query messages where admin column is set to true, so that
		 * it only receives notifications on messages from an agent not an echo
		 * of its own client message
		 */
		query.equalTo("admin", false);
		try {
			CloudObject.on("chat", "created", query, new CloudObjectCallback() {

				@Override
				public void done(final CloudObject arg0, CloudException arg1)
						throws CloudException {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							receiveMessageFromClient(arg0);

						}
					});

				}
			});
		} catch (CloudException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * this method takes a cloudobject and retrieves 'message' out of it and
	 * adds it to the adapter
	 * 
	 * @param object
	 * @return
	 */
	private boolean receiveMessageFromClient(CloudObject object) {
		String msg = object.getString("message");

		chatArrayAdapter.add(new ChatMessage(true, msg));
		return true;
	}

	/**
	 * runs a background service to send our message
	 * 
	 * @return
	 */
	private boolean sendMessageToClient() {
		new sendMsg().execute(new String[] {});

		return true;
	}

	ProgressDialog pDialog;

	public void runProgressGuy(String msg) {
		pDialog = new ProgressDialog(this);
		pDialog.setMessage(msg);
		pDialog.setIndeterminate(false);
		pDialog.setCancelable(false);
		pDialog.show();
	}

	class sendMsg extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			runProgressGuy("Sending...");
		}

		/**
		 * send message on a background thread
		 * */
		@Override
		protected String doInBackground(String... args) {
			CloudObject obj = new CloudObject("chat");
			try {
				obj.set("admin", true);
				obj.set("message", chatText.getText().toString());
				obj.save(new CloudObjectCallback() {

					@Override
					public void done(final CloudObject arg0,
							final CloudException arg1) throws CloudException {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (arg1 != null) {
									chatArrayAdapter.add(new ChatMessage(false,
											arg1.toString()));
									for (Object obj : arg1.getStackTrace()) {
									}
								}
								if (arg0 != null)
									chatArrayAdapter.add(new ChatMessage(false,
											chatText.getText().toString()));

							}
						});

					}
				});
			} catch (CloudException e) {
				e.printStackTrace();
			}
			runOnUiThread(new Runnable() {
				public void run() {
					chatText.setText("");
				}
			});

			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 * **/
		protected void onPostExecute(String args) {
			pDialog.dismiss();
		}
	}

}