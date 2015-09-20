package eu.unicate.android.nearby;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Created by andre on 20.09.2015.
 */
public class NearbyClient {

	private static final String TAG = NearbyClient.class.getSimpleName();
	private static final int REQUEST_RESOLVE_ERROR = 123;
	private final GoogleApiClient messagesClient;
	private final Activity activity;
	private boolean resolvingError;


	private GoogleApiClient.ConnectionCallbacks callbacks = new GoogleApiClient.ConnectionCallbacks() {
		@Override
		public void onConnected(Bundle bundle) {
			Nearby.Messages.getPermissionStatus(messagesClient).setResultCallback(
					new ResultCallback<Status>() {
						@Override
						public void onResult(Status status) {
							if (status.isSuccess()) {
								// successfully connected
								onClientConnected();
							} else {
								// Currently, the only resolvable error is that the device is not opted
								// in to Nearby. Starting the resolution displays an opt-in dialog.
								if (status.hasResolution()) {
									if (!resolvingError) {
										try {
											status.startResolutionForResult(activity, REQUEST_RESOLVE_ERROR);
											resolvingError = true;
										} catch (IntentSender.SendIntentException e) {
											Log.e(TAG, "Failed with exception: ", e);
										}
									} else {
										// This will be encountered on initial startup because we do
										// both publish and subscribe together.  So having a toast while
										// resolving dialog is in progress is confusing, so just log it.
										Log.i(TAG, "Failed with status: " + status + " while resolving error");
									}
								} else {
									Log.i(TAG, "Failed with status: " + status + " while resolving error");
								}
							}
						}
					});
		}

		@Override
		public void onConnectionSuspended(int i) {

		}
	};

	public NearbyClient(Activity activity) {
		this.activity = activity;
		messagesClient = new GoogleApiClient.Builder(activity)
				.addApi(com.google.android.gms.nearby.Nearby.MESSAGES_API)
				.addConnectionCallbacks(callbacks)
				.build();
	}

	public void connect() {
		if (!messagesClient.isConnected()) {
			messagesClient.connect();
		}
	}

	public void disconnect() {
		messagesClient.disconnect();
	}

	private void onClientConnected() {
		Log.e(TAG, "client connected");
	}


	public Observable<Message> subscribeForMessages() {
		return Observable.create(new Observable.OnSubscribe<Message>() {
			@Override
			public void call(final Subscriber<? super Message> subscriber) {
				final MessageListener messageListener = new MessageListener() {
					@Override
					public void onFound(Message message) {
						subscriber.onNext(message);
					}
				};
				subscriber.add(Subscriptions.create(new Action0() {
					@Override
					public void call() {
						Log.i(TAG, "stop listening for messages...");
						Nearby.Messages.unsubscribe(messagesClient, messageListener);
					}
				}));
				Log.i(TAG, "listening for messages...");
				Nearby.Messages.subscribe(messagesClient, messageListener);
			}
		});
	}

	public Observable<Boolean> broadcastMessage(final Message message) {
		return Observable.create(new Observable.OnSubscribe<Boolean>() {
			@Override
			public void call(final Subscriber<? super Boolean> subscriber) {
				subscriber.add(Subscriptions.create(new Action0() {
					@Override
					public void call() {
						Log.i(TAG, "unpublish");
						Nearby.Messages.unpublish(messagesClient, message);
					}
				}));
				Log.i(TAG, "publish");
				Nearby.Messages.publish(messagesClient, message).setResultCallback(new ResultCallback<Status>() {
					@Override
					public void onResult(Status status) {
						subscriber.onNext(status.isSuccess());
					}
				});
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode) {
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			resolvingError = false;
			if (resultCode == Activity.RESULT_OK) {
				// Permission granted or error resolved successfully then we proceed
				// with publish and subscribe..
				onClientConnected();
			} else {
				// This may mean that user had rejected to grant nearby permission.
				Log.e(TAG, "Failed to resolve error with code " + resultCode);
			}
		}
	}
}
