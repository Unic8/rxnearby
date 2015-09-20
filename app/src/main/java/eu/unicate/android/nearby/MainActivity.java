package eu.unicate.android.nearby;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.google.android.gms.nearby.messages.Message;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

	@Bind(R.id.inputMessage)
	EditText inputMessage;
	@Bind(R.id.toolbar)
	Toolbar toolbar;
	private NearbyClient client;
	private Subscription listeningSubscription;
	private Subscription messageSubscription;
	private CompositeSubscription subscriptions = new CompositeSubscription();
	private Switch listeningSwitch;

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
				if (messageSubscription != null) {
					messageSubscription.unsubscribe();
					subscriptions.remove(messageSubscription);
					messageSubscription = null;
				}
				messageSubscription = client.broadcastMessage(new Message(message))
						.subscribe(new Action1<Boolean>() {
							@Override
							public void call(Boolean aBoolean) {

							}
						});
				subscriptions.add(messageSubscription);
			}
		});

		client = new NearbyClient(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu, menu);
		MenuItem listenItem = menu.findItem(R.id.menuitem_switch);
		if (listenItem != null) {
			listeningSwitch = (Switch) listenItem.getActionView().findViewById(R.id.listeningSwitch);
			listeningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						listeningSubscription = client.subscribeForMessages()
								.subscribe(new Action1<Message>() {
									@Override
									public void call(Message message) {
										showMessage(message);
									}
								});
						subscriptions.add(listeningSubscription);
					} else {
						if (listeningSubscription != null) {
							listeningSubscription.unsubscribe();
							subscriptions.remove(listeningSubscription);
						}
					}
				}
			});
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		client.connect();
	}

	@Override
	protected void onPause() {
		super.onPause();
		subscriptions.unsubscribe();
		listeningSubscription = null;
		messageSubscription = null;
		client.disconnect();
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

	private void showMessage(Message message) {
		showToast(inputMessage, new String(message.getContent()));
	}

}
