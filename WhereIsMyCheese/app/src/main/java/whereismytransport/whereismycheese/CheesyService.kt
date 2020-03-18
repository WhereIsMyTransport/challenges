package whereismytransport.whereismycheese

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
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

    override fun onDestroy() {
        super.onDestroy()
        locationEngine.removeLocationUpdates(callback);
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

        if(context is MainActivity) {
            callback = LocationCallback(context)
            locationEngine.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        }
    }

    inner class CheesyBinder : Binder() {
        fun getService(): CheesyService = this@CheesyService
    }
}

private class LocationCallback(activity: Context?) : LocationEngineCallback<LocationEngineResult> {

    private var activityWeakReference = WeakReference(activity as MainActivity)

    override fun onSuccess(result: LocationEngineResult?) {
        val activity = activityWeakReference.get()

        activity.apply {
            this?.map?.locationComponent?.forceLocationUpdate(result?.lastLocation)
        }
    }

    override fun onFailure(exception: Exception) {
    }
}