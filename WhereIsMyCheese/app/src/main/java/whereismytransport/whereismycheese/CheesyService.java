package whereismytransport.whereismycheese;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * In order to help the app determine if you are near a cheezy note, you will need to use Location somehow..
 * The idea is that a service will run, constantly checking to see if you have indeed found a cheezy treasure..
 */
public class CheesyService extends Service {

    private FusedLocationProviderClient mLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerEventReceiver();
        mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = createLocationRequest();
        mLocationCallback = new LocationCallback();
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofencingClient.removeGeofences(getGeofencePendingIntent());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupForeground();
        startLocationUpdates();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMapEventReceiver);
    }

    private void setupForeground() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION.CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_search_title))
                .setSmallIcon(R.drawable.black_outline_logo)
                .setContentIntent(pendingIntent);
        startForeground(Constants.NOTIFICATION.FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void startLocationUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                Looper.getMainLooper());
    }

    private void restoreNotes() {
        Intent responseIntent = new Intent(this, MainActivity.class);
        responseIntent.setAction(Constants.ACTION.RESTORE_NOTE_RESPONSE);
        responseIntent.putExtra(Constants.KEY.NOTE, (Serializable) CheesyNoteStore.getInstance().getNoteList());
        LocalBroadcastManager.getInstance(CheesyService.this).sendBroadcast(responseIntent);
    }

    private void registerEventReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.ADD_NOTE);
        filter.addAction(Constants.ACTION.REMOVE_NOTE);
        filter.addAction(Constants.ACTION.RESTORE_NOTE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMapEventReceiver, filter);
    }

    private BroadcastReceiver mMapEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            double latitude = intent.getDoubleExtra(Constants.KEY.LATITUDE, 0);
            double longitude = intent.getDoubleExtra(Constants.KEY.LONGITUDE, 0);
            String content = intent.getStringExtra(Constants.KEY.CONTENT);

            switch (intent.getAction()) {
                case Constants.ACTION.ADD_NOTE:
                    CheesyNoteStore.getInstance().addNote(latitude, longitude, content);
                    addGeofence(latitude, longitude);
                    break;

                case Constants.ACTION.REMOVE_NOTE:
                    CheesyNoteStore.getInstance().removeNote(latitude, longitude, content);
                    removeGeofence(latitude, longitude);
                    break;

                case Constants.ACTION.RESTORE_NOTE:
                    restoreNotes();
                    break;
            }
        }
    };

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent == null) {
            Intent intent = new Intent(this, GeofenceEventReceiver.class);
            geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return geofencePendingIntent;
    }

    private void addGeofence(double latitude, double longitude) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId(createGeofenceRequestId(latitude, longitude))
                .setCircularRegion(latitude, longitude, Constants.GEOFENCE.RADIUS_IN_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofenceRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build();
        geofencingClient.addGeofences(geofenceRequest, getGeofencePendingIntent());
    }

    private void removeGeofence(double latitude, double longitude) {
        List<String> list = new ArrayList<>();
        list.add(createGeofenceRequestId(latitude, longitude));
        geofencingClient.removeGeofences(list);
    }

    private String createGeofenceRequestId(double latitude, double longitude) {
        return latitude + "," + longitude;
    }

}
