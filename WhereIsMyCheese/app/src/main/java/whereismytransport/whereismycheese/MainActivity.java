package whereismytransport.whereismycheese;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Marker> markers = new ArrayList<>();

    private MapView mapView;
    private MapboxMap map;

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Mapbox.getInstance(this, "pk.eyJ1IjoiYXRtbmciLCJhIjoiY2s5Mno5bDVxMDA3cDNnbnBoYXVpeHB3aiJ9.uaH2HfpqQEh9bG2bMNDAcw");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // One does not simply just cheez, you require some permissions.
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            initializeMap();
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
                MainActivity.this.map = mapboxMap;
                setupLongPressListener();
            }
        });
    }

    private void setupLongPressListener() {
        map.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng point) {
                createCheesyNote(point);
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

    private void addCheeseToMap(LatLng point, String content) {
        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        Icon icon = iconFactory.fromBitmap(getBitmapFromDrawableId(R.drawable.cheese64));
        MarkerOptions marker = new MarkerOptions();
        marker.setIcon(icon);
        marker.setPosition(point);
        marker.setTitle(content);
        markers.add(map.addMarker(marker));
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
