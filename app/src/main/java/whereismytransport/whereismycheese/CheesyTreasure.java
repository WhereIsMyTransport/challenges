package whereismytransport.whereismycheese;

import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Cheezy Treasures are such a delight
 */
public class CheesyTreasure {
    private LatLng location;
    private String note;

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LatLng getLocation() {
        return this.location;
    }

    public String getNote() {
        return note;
    }

    public CheesyTreasure(LatLng location, String content) {
        this.setLocation(location);
        this.setNote(content);
    }

    // For simplicities sake, we will just assume all cheesy treasures have unique notes
    public boolean equals(CheesyTreasure other) {
        return this.note.equals(other.note);
    }
}
