# rxnearby
This is my android nearby playground.

How to use:

* Install the app on at least 2 devices
* switch on the "Listen" switch on one (or more) devices
* type in a text into the edittext and press the FAB to publish a message. This message will be published as long as 
  * you don't send a new message
  * pause the activity
* On the listening devices, this message should show once as snackbar text


Issues:
 * Don't try to send/receive messages, as long as the client is not connected. this will crash!
 * when pause-resuming the app, sending messages doesn't work anymore. for some reason it's publishing and unpublish right after it.
