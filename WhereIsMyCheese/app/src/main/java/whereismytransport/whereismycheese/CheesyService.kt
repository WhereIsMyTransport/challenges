package whereismytransport.whereismycheese

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder

import com.mapbox.android.core.location.*
import java.lang.ref.WeakReference


class CheesyService : Service() {

    companion object {
        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 2000L;
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 1;
    }

    private lateinit var locationEngine: LocationEngine
    private lateinit var callback: LocationCallback

    private val binder = CheesyBinder()
    private val treasureChest: MutableList<CheesyTreasure> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        startForeground(1, getNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        locationEngine.removeLocationUpdates(callback)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun addCheeseToChest(treasure: CheesyTreasure) {
        treasureChest.add(treasure)
    }

    fun setupLocationService(context: Context) {
        locationEngine = LocationEngineProvider.getBestLocationEngine(context)

        val locationRequest = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                .build()

        if (context is MainActivity) {
            callback = LocationCallback(context)
            locationEngine.requestLocationUpdates(locationRequest, callback, null)
        }
    }

    inner class CheesyBinder : Binder() {
        fun getService(): CheesyService = this@CheesyService
    }




    inner class LocationCallback(context: Context?) : LocationEngineCallback<LocationEngineResult> {

        private var activityWeakReference = WeakReference(context as MainActivity)

        override fun onSuccess(result: LocationEngineResult?) {
            activityWeakReference.get().apply {
                //Implementing custom location callback requires forcing the location update on the mapBoxMap
                this?.map?.locationComponent?.forceLocationUpdate(result?.lastLocation)

                treasureChest.forEachIndexed { index, cheesyTreasure ->
                    val distance = result?.lastLocation?.distanceTo(cheesyTreasure.location)?.compareTo(50.00f) ?: 1
                    if (distance < 0) {
                        this?.notifyCheeseFound(cheesyTreasure)
                        treasureChest.removeAt(index)
                        this?.removeTastyCheeseFromMap(cheesyTreasure.marker)
                        return@apply
                    }
                }
            }
        }

        override fun onFailure(exception: Exception) {

        }
    }

    private fun getNotification(): Notification? {
        val channel = NotificationChannel("CheesyService", "CheesyServiceChannel", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
        val builder = Notification.Builder(applicationContext, "CheesyService").setAutoCancel(true)
        return builder.build()
    }
}