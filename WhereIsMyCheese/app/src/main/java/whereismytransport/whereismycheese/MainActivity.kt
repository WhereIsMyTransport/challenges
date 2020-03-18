package whereismytransport.whereismycheese

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import whereismytransport.whereismycheese.CheesyDialog.INoteDialogListener
import java.util.*

class MainActivity : AppCompatActivity() {

    private val markers: MutableList<Marker> = ArrayList()

    private lateinit var mapView: MapView
    lateinit var map: MapboxMap
    private lateinit var cheeseService: CheesyService
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CheesyService.CheesyBinder
            cheeseService = binder.getService()
            serviceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

        Intent(this, CheesyService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        unbindService(connection)
        serviceBound = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "sk.eyJ1IjoiZ3V5c2F3eWVyIiwiYSI6ImNrN3U3bmpvYzB6Ym0zbG1yenEwaHl4cWwifQ.ZlJrtoCmtFS4ji0Eu-gRag")
        setContentView(R.layout.activity_main)

        mapView = findViewById<View>(R.id.mapView) as MapView
        mapView.onCreate(savedInstanceState)

        checkLocationPermission()
    }

    private fun checkLocationPermission() { // One does not simply just cheez, you require some permissions.
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            initializeMap()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    Constants.PERMISSIONS.ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.PERMISSIONS.ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //Permission granted
                    initializeMap()
                } else { // permission denied, boo! Fine, no cheese for you!
// No need to do anything here, for this exercise we only care about people who like cheese and have location setting on.
                }
            }
        }
    }

    private fun initializeMap() {
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap
            map.setStyle(Style.MAPBOX_STREETS) { style ->
                setupLongPressListener()
                enableLocationComponent(style)
                cheeseService.setupLocationService(this)
            }
        }
    }

    private fun enableLocationComponent(style: Style) {
        val locationComponent = map.locationComponent
        val locationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .build()
        locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, style)
                .useDefaultLocationEngine(false)
                .locationComponentOptions(locationComponentOptions)
                .build())
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING_COMPASS

        locationComponent.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
            override fun onCameraTrackingDismissed() {
                locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
            }
            override fun onCameraTrackingChanged(currentMode: Int) {}
        })
    }

    private fun setupLongPressListener() {
        map.addOnMapLongClickListener { point ->
            createCheesyNote(point)
            true
        }
    }

    private fun createCheesyNote(point: LatLng) {
        val note = CheesyDialog(this, INoteDialogListener { note -> addCheeseToMap(point, note) })
        note.show()
    }

    private fun addCheeseToMap(point: LatLng, content: String) {
        val iconFactory = IconFactory.getInstance(this@MainActivity)
        val icon = iconFactory.fromBitmap(R.drawable.cheese64.getBitmapFromDrawableId()!!)
        val marker = MarkerOptions()
        marker.icon = icon
        marker.position = point
        marker.title = content
        markers.add(map.addMarker(marker))

        cheeseService.addCheeseToChest(CheesyTreasure(point, content))
    }

    private fun Int.getBitmapFromDrawableId(): Bitmap? {
        val vectorDrawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            resources.getDrawable(this, null)
        } else {
            resources.getDrawable(this)
        }
        val wrapDrawable = DrawableCompat.wrap(vectorDrawable)
        var h = vectorDrawable.intrinsicHeight
        var w = vectorDrawable.intrinsicWidth
        h = if (h > 0) h else 96
        w = if (w > 0) w else 96
        wrapDrawable.setBounds(0, 0, w, h)
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        wrapDrawable.draw(canvas)
        return bm
    }
}