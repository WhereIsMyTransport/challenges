package whereismytransport.whereismycheese;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MapView mapView;
    protected MapboxMap map;
    LocationComponent locationComponent;

    private Intent backgroundGeoServiceIntent;
    private GeoService mService;

    /**
     * Defines callbacks for service binding, passed to bindService().
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((GeoService.RunServiceBinder) service).getService();

            // The app is now in the foreground, hide the service notification in the system area
             mService.background();

            if (checkPermission()) {
                initializeMap();
            } else {
                askPermission();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        startBackgroundGeoService();
    }

    void startBackgroundGeoService() {
        if (mService == null) {
            if (backgroundGeoServiceIntent == null) {
                backgroundGeoServiceIntent = new Intent(this, GeoService.class);
            }
            startService(backgroundGeoServiceIntent);
            bindService(backgroundGeoServiceIntent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        if (extras.containsKey(getString(R.string.geofence_start_activity))) {
            String id = extras.getString(getString(R.string.geofence_start_activity));

            if (id == null || id.isEmpty()) {
                return;
            }

            if (mService != null && mService.isServiceRunning()) {
                mService.removeGeofence(id, this);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        if (outState != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (backgroundGeoServiceIntent == null) {
            backgroundGeoServiceIntent = new Intent(this, GeoService.class);
        }

        bindService(backgroundGeoServiceIntent, connection, Context.BIND_AUTO_CREATE);

        mapView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();

        mapView.onStop();

        // As the user navigates away from this app, if the background service is active,
        if (mService != null) {
            // show the background service notification in the systems notifications area.
            if (mService.isServiceRunning()) {
                mService.foreground();
            } else {
                // If, on the other hand, the background service is is not active, disconnect the
                // UI from it completely and ask the system to kill the service.
                unbindService(connection);
                stopService(backgroundGeoServiceIntent);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        unbindService(connection);
    }

    /**
     * Check if permission has been granted to this app to access Location Services.
     * @return True or false.
     */
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    /**
     * Request for permission to access Location Services.
     */
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                Constants.PERMISSIONS.ACCESS_FINE_LOCATION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == Constants.PERMISSIONS.ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission granted
                initializeMap();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This app requires access to your location to work properly.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeMap() {
        if (map != null) {
            return;
        }

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                map = mapboxMap;
                map.setStyle(Style.LIGHT,
                        new Style.OnStyleLoaded() {
                            @Override
                            public void onStyleLoaded(@NonNull Style style) {
                                enableLocationGuideVisualsOnMap(style);
                                mService.startLocationMonitoring(MainActivity.this);
                            }
                        });

                setupLongPressListener();
            }
        });
    }

    /**
     * Activate the map UI affordances that show user where they are on the map, including an
     * indicator (for showing the user's location and bearing -- direction the user is facing) and a
     * camera mode (providing intuitive animations that move the map while keeping the user
     * indicator centered on the map).
     *
     * @param loadedMapStyle The map style loaded for the map.
     */
    private void enableLocationGuideVisualsOnMap(@NonNull Style loadedMapStyle) {
        if (locationComponent != null) {
            return;
        }

        // Get an instance of the component
        locationComponent = map.getLocationComponent();

        // Set the LocationComponent activation options
        LocationComponentActivationOptions locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                        .useDefaultLocationEngine(false)
                        .build();

        // Activate with the LocationComponentActivationOptions object
        locationComponent.activateLocationComponent(locationComponentActivationOptions);

        // Enable to make component visible
        locationComponent.setLocationComponentEnabled(true);

        // Set the component's camera mode
        locationComponent.setCameraMode(CameraMode.TRACKING);

        // Set the component's render mode
        locationComponent.setRenderMode(RenderMode.COMPASS);
    }

    private void setupLongPressListener() {
        map.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public boolean onMapLongClick(@NonNull LatLng point) {
                createCheesyNote(point);
                return true;
            }
        });
    }

    private void createCheesyNote(final LatLng point) {
        CheesyDialog note = new CheesyDialog(this, new CheesyDialog.INoteDialogListener() {
            @Override
            public void onNoteAdded(String note) {
                addCheeseToMap(point, note);
            }
        });
        note.show();
    }

    private void addCheeseToMap(LatLng point, final String content) {
        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        Icon icon = iconFactory.fromBitmap(getBitmapFromDrawableId(R.drawable.cheese64));
        final MarkerOptions marker = new MarkerOptions();
        marker.setIcon(icon);
        marker.setPosition(point);
        marker.setTitle(content);

        mService.addGeofence(
                this,
                point.getLatitude(),
                point.getLongitude(),
                marker,
                content);
    }

    private Bitmap getBitmapFromDrawableId(int drawableId) {
        Drawable vectorDrawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vectorDrawable = getResources().getDrawable(drawableId, null);
        } else {
            vectorDrawable = getResources().getDrawable(drawableId);
        }

        Drawable wrapDrawable = DrawableCompat.wrap(vectorDrawable);

        int h = vectorDrawable.getIntrinsicHeight();
        int w = vectorDrawable.getIntrinsicWidth();

        h = h > 0 ? h : 96;
        w = w > 0 ? w : 96;

        wrapDrawable.setBounds(0, 0, w, h);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        wrapDrawable.draw(canvas);
        return bm;
    }
}
