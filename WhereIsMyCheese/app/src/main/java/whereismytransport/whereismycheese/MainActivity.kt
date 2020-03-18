package whereismytransport.whereismycheese

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
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
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import whereismytransport.whereismycheese.CheesyDialog.INoteDialogListener
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CHANNEL_ID = "WhereIsMyCheese"
    }


    lateinit var map: MapboxMap

    private val markers: MutableList<Marker> = ArrayList()
    private var serviceBound = false
    private lateinit var mapView: MapView
    private lateinit var cheeseService: CheesyService

    private val connection = object : ServiceConnection {




        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CheesyService.CheesyBinder
            cheeseService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()




    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        cheeseService.onDestroy()
        mapView.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        Intent(this, CheesyService::class.java).also { intent ->
            startForegroundService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        Mapbox.getInstance(this, "sk.eyJ1IjoiZ3V5c2F3eWVyIiwiYSI6ImNrN3U3bmpvYzB6Ym0zbG1yenEwaHl4cWwifQ.ZlJrtoCmtFS4ji0Eu-gRag")
        setContentView(R.layout.activity_main)

        mapView = findViewById<View>(R.id.mapView) as MapView
        mapView.onCreate(savedInstanceState)

        checkLocationPermission()
    }

    private fun checkLocationPermission() { // One does not simply just cheez, you require some permissions.
        val permissionAccessFineLocationApproved = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionAccessFineLocationApproved == PackageManager.PERMISSION_GRANTED) {
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

        locationComponent.apply {
            activateLocationComponent(LocationComponentActivationOptions.builder(this@MainActivity, style)
                    .useDefaultLocationEngine(false)
                    .locationComponentOptions(locationComponentOptions)
                    .build())
            isLocationComponentEnabled = true
            cameraMode = CameraMode.TRACKING_COMPASS
            renderMode = RenderMode.COMPASS
        }

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
        val note = CheesyDialog(this, object : INoteDialogListener {
            override fun onNoteAdded(note: String) {
                addCheeseToMap(point, note)
            }
        })
        note.show()
    }

    private fun addCheeseToMap(point: LatLng, content: String) {
        val iconFactory = IconFactory.getInstance(this@MainActivity)
        val marker = MarkerOptions().apply {
            icon = iconFactory.fromBitmap(R.drawable.cheese64.getBitmapFromDrawableId())
            position = point
            title = content
        }
        markers.add(marker.marker)
        map.addMarker(marker)

        val loc = Location("MainActivity").apply {
            latitude = point.latitude
            longitude = point.longitude
        }
        cheeseService.addCheeseToChest(CheesyTreasure(loc, markers.last(), content, markers.size + 1))
    }

    private fun Int.getBitmapFromDrawableId(): Bitmap {
        val vectorDrawable = resources.getDrawable(this, null)




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

    fun removeTastyCheeseFromMap(marker: Marker) {
        markers.remove(marker)
        map.removeMarker(marker)
    }

    fun notifyCheeseFound(cheese: CheesyTreasure) {
        //TODO Currently the notification provides all details of the note
        //TODO This can be used later if we want to open the app and display the note details there.
//        val intent: Intent = Intent(this, MainActivity::class.java).apply {
//            action = Intent.ACTION_MAIN;
//            addCategory(Intent.CATEGORY_LAUNCHER);
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        }
//        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.cheese64)
                .setContentTitle("Found Cheese")
                .setContentText("Found a cheesy treat!")
                .setStyle(Notification.BigTextStyle().bigText("Found a cheesy treat at ${cheese.location.longitude} Long and ${cheese.location.latitude} Latitude and it says '${cheese.note}'"))
                //TODO Uncomment to use the pending intent from the notification.
//                .setContentIntent(pendingIntent)
                .setAutoCancel(false)

        with(NotificationManagerCompat.from(this)) {
            notify(cheese.noteId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}