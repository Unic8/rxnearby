package eu.unicate.android.nearby;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.nearby.messages.Message;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

	@Bind(R.id.inputMessage)
	EditText inputMessage;
	@Bind(R.id.toolbar)
	Toolbar toolbar;
	private NearbyClient client;
	private CompositeSubscription subscriptions = new CompositeSubscription();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String textMessage = inputMessage.getText().toString();
				byte[] message = textMessage.getBytes();
				subscriptions.add(client.broadcastMessage(new Message(message))
						.subscribeOn(Schedulers.io())
						.subscribe(
								new Action1<Boolean>() {
									@Override
									public void call(Boolean status) {

									}
								}
						));
				subscriptions.add(client.subscribeForMessages()
						.subscribeOn(Schedulers.io())
						.subscribe(new Action1<Message>() {
							@Override
							public void call(Message message) {
								showToast(inputMessage, new String(message.getContent()));
							}
						}));

			}
		});

		client = new NearbyClient(this);
		client.connect();
	}

	@Override
	protected void onStop() {
		subscriptions.unsubscribe();
		client.disconnect();
		super.onStop();
	}

	// This is called in response to a button tap in the NearbyClient permission dialog.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		client.onActivityResult(requestCode, resultCode);
	}

	private void showToast(View view, String message) {
		Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
	}

}
