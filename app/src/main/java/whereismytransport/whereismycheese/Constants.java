package whereismytransport.whereismycheese;

/**
 * Cheez is always the best when melted.. Here we just have some constants, to keep the code more cheezy
 */
class Constants {
    public interface PERMISSIONS {
        int ACCESS_FINE_LOCATION = 16158;
    }

    final static int GEOFENCE_REQUEST_CODE = 0;
    final static int GEOFENCE_NOTIFICATION_ID = 0;
    final static String NOTIFICATION_CHANNEL_ID = "channel_01";

    // Foreground notification id
    final static int FOREGROUND_NOTIFICATION_ID = 1;

    final static float GEOFENCE_RADIUS_IN_METERS = 50f;

    // 3 minutes interval
    final static long LOCATION_UPDATE_INTERVAL = 1000L;

    // Every minute (60 seconds)
    final static long LOCATION_FASTEST_INTERVAL = 60 * 1000;

    // One hour
    final static long LOCATION_MAX_WAIT_TIME = 1000L;
}
