package whereismytransport.whereismycheese;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Marker> markers = new ArrayList<>();

    private MapView mapView;
    private MapboxMap map;

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRestoreDataReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private BroadcastReceiver mRestoreDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(CheesyService.ACTION_RESTORE_NOTE_RESPONSE)) {
                Toast.makeText(MainActivity.this, intent.getAction(), Toast.LENGTH_SHORT).show();

                List<CheesyNoteStore.CheesyNote> noteList = (List<CheesyNoteStore.CheesyNote>) intent.getSerializableExtra("note");
                for (CheesyNoteStore.CheesyNote note : noteList) {
                    LatLng point = new LatLng(note.latitude, note.longitude);
                    addCheeseToMap(point, note.content);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mRestoreDataReceiver, new IntentFilter(CheesyService.ACTION_RESTORE_NOTE_RESPONSE));

        Mapbox.getInstance(this, "pk.eyJ1IjoiYXRtbmciLCJhIjoiY2s5Mno5bDVxMDA3cDNnbnBoYXVpeHB3aiJ9.uaH2HfpqQEh9bG2bMNDAcw");
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // One does not simply just cheez, you require some permissions.
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            initializeMap();
            startService(new Intent(this, CheesyService.class));
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.PERMISSIONS.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSIONS.ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission granted
                    initializeMap();
                    startService(new Intent(this, CheesyService.class));
                } else {
                    // permission denied, boo! Fine, no cheese for you!
                    // No need to do anything here, for this exercise we only care about people who like cheese and have location setting on.
                }
                return;
            }
        }
    }

    private void initializeMap() {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                Style.Builder style = new Style.Builder().fromUri(getResources().getString(R.string.mapbox_style_light));
                mapboxMap.setStyle(style, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enabledLocationComponent(style);
                    }
                });
                MainActivity.this.map = mapboxMap;
                setupClickListener();
                restoreNotes();
            }
        });
    }

    private void enabledLocationComponent(Style style) {
        final LocationComponent locationComponent = map.getLocationComponent();
        locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, style).useDefaultLocationEngine(true).build());
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setCameraMode(CameraMode.TRACKING);
        locationComponent.setRenderMode(RenderMode.COMPASS);
    }

    private void setupClickListener() {
        map.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public boolean onMapLongClick(@androidx.annotation.NonNull LatLng point) {
                createCheesyNote(point);
                return true;
            }
        });
        map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                showNote(marker);
                return true;
            }

        });
    }

    private void createCheesyNote(final LatLng point) {
        CheesyDialog note = new CheesyDialog(this, new CheesyDialog.INoteDialogListener() {
            @Override
            public void onNoteAdded(String note) {
                addCheeseToMap(point, note);
                saveNote(point, note);
            }
        });
        note.show();
    }

    private void showNote(final Marker marker) {
        CheesyDiscoverDialog dialog = new CheesyDiscoverDialog(this, marker.getTitle(), new CheesyDiscoverDialog.IDiscoverDialogListener() {
            @Override
            public void onNoteDiscovered() {
                removeCheeseFromMap(marker);
                removeNote(marker.getPosition(), marker.getTitle());
            }
        });
        dialog.show();
    }

    private void addCheeseToMap(LatLng point, String content) {
        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        Icon icon = iconFactory.fromBitmap(getBitmapFromDrawableId(R.drawable.cheese64));
        MarkerOptions marker = new MarkerOptions();
        marker.setIcon(icon);
        marker.setPosition(point);
        marker.setTitle(content);
        markers.add(map.addMarker(marker));
    }

    private void removeCheeseFromMap(Marker marker) {
        map.removeMarker(marker);
        markers.remove(marker);
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

    private void saveNote(LatLng point, String content) {
        Intent intent = new Intent(CheesyService.ACTION_ADD_NOTE);
        intent.putExtra("lat", point.getLatitude());
        intent.putExtra("lon", point.getLongitude());
        intent.putExtra("content", content);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void removeNote(LatLng point, String content) {
        Intent intent = new Intent(CheesyService.ACTION_REMOVE_NOTE);
        intent.putExtra("lat", point.getLatitude());
        intent.putExtra("lon", point.getLongitude());
        intent.putExtra("content", content);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void restoreNotes() {
        Intent intent = new Intent(CheesyService.ACTION_RESTORE_NOTE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
