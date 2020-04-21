package whereismytransport.whereismycheese;

/**
 * Cheez is always the best when melted.. Here we just have some constants, to keep the code more cheezy
 */
public class Constants {
    public interface PERMISSIONS {
        public static int ACCESS_FINE_LOCATION = 16158;
    }

    public interface ACTION {
        public static String ADD_NOTE = "ADD_NOTE";
        public static String REMOVE_NOTE = "REMOVE_NOTE";
        public static String RESTORE_NOTE = "RESTORE_NOTE";
        public static String RESTORE_NOTE_RESPONSE = "RESPONSE";
    }

    public interface KEY {
        public static String LATITUDE = "lat";
        public static String LONGITUDE = "lon";
        public static String CONTENT = "content";
        public static String NOTE = "note";
    }

    public interface GEOFENCE {
        public static final float RADIUS_IN_METERS = 50;
    }

    public interface NOTIFICATION {
        public static final String CHANNEL_ID = "100";
        public static final int NOTIFICATION_ID = 100;
        public static final int FOREGROUND_NOTIFICATION_ID = 200;
    }

}
