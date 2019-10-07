package whereismytransport.whereismycheese;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static whereismytransport.whereismycheese.Constants.NOTIFICATION_CHANNEL_ID;

/**
 * In order to help the app determine if you are near a cheezy note, you will need to use Location somehow..
 * The idea is that a service will run, constantly checking to see if you have indeed found a cheezy treasure..
 */
public class CheesyService extends IntentService {

    private static final String TAG = CheesyService.class.getSimpleName();

    public CheesyService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve the geofence intent
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        // Handling errors
        if (geofencingEvent.hasError()) {
            String errorMsg = getErrorStringForCode(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMsg);
            return;
        }

        // Get the transition type
        int geoFenceTransition = geofencingEvent.getGeofenceTransition();

        // Check the transition type
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Create a detail message with Geofences received
            String geofenceTransitionDetails =
                    getGeofenceTransitionDetails(triggeringGeofences);

            // Send notification details as a String
            sendNotification(geofenceTransitionDetails);
        }
    }

    /**
     * Create a detail message with Geofences received
     * @param triggeringGeofences List of geofences that were triggered.
     * @return Detail message created from the list of geofences triggered.
     */
    private String getGeofenceTransitionDetails(List<Geofence> triggeringGeofences) {
        // Get the ID of each geofence triggered
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();

        for (Geofence geofence : triggeringGeofences) {
            String geofenceId = geofence.getRequestId();
            triggeringGeofencesList.add(geofenceId);
        }

        // Return the last triggered geofence
        return triggeringGeofencesList.get(0);
    }

    /**
     * Compose and trigger a system-area notification announcing geofence triggered.
     * @param geofenceId ID of the geofence triggered.
     */
    private void sendNotification(String geofenceId) {
        Log.i(TAG, "sendNotification: " + geofenceId );

        // Intent to start the main Activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(getString(R.string.geofence_start_activity), geofenceId);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(notificationIntent);

        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT);

        // Creating and sending Notification
        NotificationManager notificationMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription(geofenceId);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            notificationMgr.createNotificationChannel(channel);
        }

        notificationMgr.notify(
                Constants.GEOFENCE_NOTIFICATION_ID,
                createNotification(geofenceId, notificationPendingIntent));
    }

    /**
     * Create a notification
     * @param msg
     * @param notificationPendingIntent Pending intent.
     * @return Notification
     */
    private Notification createNotification(String msg, PendingIntent notificationPendingIntent) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder
                .setSmallIcon(R.drawable.white_outline_logo)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.white_outline_logo))
                .setContentTitle("You've Found Cheese!")
                .setContentText("Hurray! You've found a cheesy note.")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(
                        Notification.DEFAULT_LIGHTS |
                        Notification.DEFAULT_VIBRATE |
                        Notification.DEFAULT_SOUND
                )
                // Dismiss notification once the user touches it.
                .setAutoCancel(true);

        return notificationBuilder.build();
    }

    /**
     * Return appropriate error message for the provided error code.
     * @param errorCode Error code.
     * @return An appropriate error message.
     */
    private static String getErrorStringForCode(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeoFence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many GeoFences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";
        }
    }
}
