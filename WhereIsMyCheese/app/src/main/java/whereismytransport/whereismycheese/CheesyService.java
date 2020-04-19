package whereismytransport.whereismycheese;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * In order to help the app determine if you are near a cheezy note, you will need to use Location somehow..
 * The idea is that a service will run, constantly checking to see if you have indeed found a cheezy treasure..
 */
public class CheesyService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
