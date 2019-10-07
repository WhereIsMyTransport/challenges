package whereismytransport.whereismycheese;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;
import static whereismytransport.whereismycheese.Constants.FOREGROUND_NOTIFICATION_ID;
import static whereismytransport.whereismycheese.Constants.NOTIFICATION_CHANNEL_ID;

public class GeoService extends Service {
    private static final String TAG = GeoService.class.getSimpleName();

    // Is this service running?
    private boolean isServiceRunning;


    LocationChangeListeningActivityLocationCallback callback;
    private LocationEngine locationEngine;

    private GeofencingClient geofencingClient;
    private PendingIntent geoFencePendingIntent;

    private List<MarkerOptions> markers = new ArrayList<>();
    public static List<Map<String, String>> cheeseMarkers = new ArrayList<>();

    // Service binder
    private final IBinder serviceBinder = new RunServiceBinder();

    class RunServiceBinder extends Binder {
        GeoService getService() {
            return GeoService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "Creating service");
        isServiceRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "Starting service");
        isServiceRunning = true;
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Binding service");
        return serviceBinder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service");

        super.onDestroy();

        locationEngine.removeLocationUpdates(callback);
        locationEngine = null;
        callback = null;

        if (isServiceRunning) {
            isServiceRunning = false;
        }

        stopSelf();
    }

    /**
     * Starts monitoring location changes and updating the map in the activity (if activity is in
     * the foreground). This method is invoked from the MainActivity very early in its lifecycle.
     * @param activity Reference to the MainActivity object.
     */
    void startLocationMonitoring(MainActivity activity) {
        if (locationEngine != null) {
            return;
        }

        if (callback == null) {
            callback = new LocationChangeListeningActivityLocationCallback(activity);
        }

        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request =
                new LocationEngineRequest.Builder(Constants.LOCATION_UPDATE_INTERVAL)
                        .setMaxWaitTime(Constants.LOCATION_MAX_WAIT_TIME)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());

        locationEngine.getLastLocation(callback);
    }

    /**
     * MapBox's location change listener callback implementation.
     */
    private class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MainActivity> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(MainActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location
         * changes.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MainActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.map != null && result.getLastLocation() != null) {
                    activity.map.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location
         * can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d(TAG, exception.getLocalizedMessage());
            MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @return Whether the service is running
     */
    public boolean isServiceRunning() {
        return isServiceRunning;
    }

    /**
     * Return the service to the background
     */
    public void background() {
        stopForeground(true);
    }

    /**
     * Bring the service into the foreground, showing a status bar notification in the process.
     */
    public void foreground() {
        startForeground(FOREGROUND_NOTIFICATION_ID, getForegroundNotification());
    }

    /**
     * Compose and return a status bar notifcation object that indicates the app is running in the
     * background.
     * @return The Notification object.
     */
    Notification getForegroundNotification() {
        String notificationTitle = "WhereIsMyCheese Still Running";
        String notificationMessage = "Tap to re-open the app.";

        Intent activityLaunchIntent = new Intent(this, MainActivity.class);
        activityLaunchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent notificationPendingIntent =
                PendingIntent.getActivity(this, 0, activityLaunchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID, notificationTitle,
                            NotificationManager.IMPORTANCE_NONE);

            notificationChannel.setDescription(notificationMessage);
            notificationChannel.enableVibration(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            manager.createNotificationChannel(notificationChannel);
        }

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.cheese_logo)
                .setContentTitle(notificationTitle)
                .setContentText(notificationMessage)
                .setContentIntent(notificationPendingIntent)
                .setPriority(PRIORITY_MIN)
                .build();
    }

    /**
     * Return an instance of the Geofencing API client object.
     * @return The Geofencing object.
     */
    GeofencingClient getGeofencingClient() {
        if (geofencingClient == null) {
            geofencingClient = LocationServices.getGeofencingClient(this);
        }
        return geofencingClient;
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        if (geoFencePendingIntent != null) {
            return geoFencePendingIntent;
        }

        Intent intent = new Intent(this, CheesyService.class);

        geoFencePendingIntent = PendingIntent.getService(
                this,
                Constants.GEOFENCE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return geoFencePendingIntent;
    }

    /**
     * Add a geofence from a marker pinned by user on the map.
     * @param activity Reference to the MainActivity, so as to send notifications (Toast) to the UI
     *                 indicating success or failure of the geofence add operation..
     * @param latitude Latitude of the geofence area.
     * @param longitude Longitude of the geofence area.
     * @param mapMarker Marker dropped on the map.
     * @param content User-entered text for the marker.
     */
    void addGeofence(MainActivity activity,
                     double latitude,
                     double longitude,
                     final MarkerOptions mapMarker,
                     final String content) {

        final MainActivity activityRef = (new WeakReference<>(activity)).get();

        final String markerHashCode = String.valueOf(mapMarker.hashCode());

        Geofence geofence = new Geofence.Builder()
                .setRequestId(content)
                //.setRequestId(markerHashCode)
                .setCircularRegion(
                        latitude,
                        longitude,
                        Constants.GEOFENCE_RADIUS_IN_METERS
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofence(geofence)
                .build();

        getGeofencingClient().addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(activity, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        activityRef.map.addMarker(mapMarker);

                        markers.add(mapMarker);

                        Map<String, String> entry = new HashMap<>();
                        entry.put(markerHashCode, content);
                        cheeseMarkers.add(entry);
                    }
                })
                .addOnFailureListener(activity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(
                                activityRef,
                                "Unable to create a geofence for cheese marker. Try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Removes a previously set geofence as well as a map marker pinned to the map indicating the
     * geofenced point.
     *
     * @param geofenceId A unique identifier for the Geofence.
     * @param activity  Reference to the MainActivity instance, so as to send notifications (Toast)
     *                  indicating success or failure of the geofence removal operation.
     */
    void removeGeofence(final String geofenceId, MainActivity activity) {
        List<String> geofences = Collections.singletonList(geofenceId);

        final MainActivity mainActivityWeakRef = (new WeakReference<>(activity)).get();

        getGeofencingClient().removeGeofences(geofences)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        for (MarkerOptions m : markers) {
                            if (m.getTitle().equals(geofenceId)) {
                                mainActivityWeakRef.map.removeMarker(m.getMarker());
                                break;
                            }
                        }

                        Toast.makeText(
                                mainActivityWeakRef,
                                "Removed CheeseNote: " + geofenceId,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(
                                mainActivityWeakRef,
                                "Error encountered while removing CheeseNote: " + geofenceId
                                        + ".\n\n" + e.getLocalizedMessage(),

                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
