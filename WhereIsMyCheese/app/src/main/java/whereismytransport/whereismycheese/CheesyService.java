package whereismytransport.whereismycheese;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.Serializable;

/**
 * In order to help the app determine if you are near a cheezy note, you will need to use Location somehow..
 * The idea is that a service will run, constantly checking to see if you have indeed found a cheezy treasure..
 */
public class CheesyService extends Service {

    private FusedLocationProviderClient mLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    public static final String ACTION_ADD_NOTE = "ADD_NOTE";
    public static final String ACTION_REMOVE_NOTE = "REMOVE_NOTE";
    public static final String ACTION_RESTORE_NOTE = "RESTORE_NOTE";
    public static final String ACTION_RESTORE_NOTE_RESPONSE = "RESPONSE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerEventReceiver();

        if (mLocationClient == null) {
            mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }
        if (mLocationRequest == null) {
            mLocationRequest = createLocationRequest();
        }
        if (mLocationCallback == null) {
            mLocationCallback = new LocationCallback();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startLocationUpdates();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMapEventReceiver);
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
        responseIntent.setAction(ACTION_RESTORE_NOTE_RESPONSE);
        responseIntent.putExtra("note", (Serializable) CheesyNoteStore.getInstance().getNoteList());
        LocalBroadcastManager.getInstance(CheesyService.this).sendBroadcast(responseIntent);
    }

    private void registerEventReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD_NOTE);
        filter.addAction(ACTION_REMOVE_NOTE);
        filter.addAction(ACTION_RESTORE_NOTE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMapEventReceiver, filter);
    }

    private BroadcastReceiver mMapEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            switch (intent.getAction()) {
                case ACTION_ADD_NOTE:
                    CheesyNoteStore.getInstance().addNote(
                            intent.getDoubleExtra("lat", 0), intent.getDoubleExtra("lon", 0), intent.getStringExtra("content"));
                    break;

                case ACTION_REMOVE_NOTE:
                    CheesyNoteStore.getInstance().removeNote(
                            intent.getDoubleExtra("lat", 0), intent.getDoubleExtra("lon", 0), intent.getStringExtra("content"));
                    break;

                case ACTION_RESTORE_NOTE:
                    restoreNotes();
                    break;
            }
        }
    };
}
