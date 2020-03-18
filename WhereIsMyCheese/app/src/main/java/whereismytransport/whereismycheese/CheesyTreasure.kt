package whereismytransport.whereismycheese

import android.location.Location
import com.mapbox.mapboxsdk.annotations.Marker

data class CheesyTreasure(var location: Location, var marker: Marker, var note: String, var noteId: Int)