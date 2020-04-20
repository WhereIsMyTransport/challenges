package whereismytransport.whereismycheese;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CheesyNoteStore {

    static CheesyNoteStore cheesyNoteStore;

    public static CheesyNoteStore getInstance() {
        if (cheesyNoteStore == null) {
            cheesyNoteStore = new CheesyNoteStore();
        }
        return cheesyNoteStore;
    }

    List<CheesyNote> noteList = new ArrayList<>();

    class CheesyNote implements Serializable {
        final double latitude;
        final double longitude;
        String content;

        private CheesyNote(double latitude, double longitude, String content) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.content = content;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (obj instanceof CheesyNote
                    && latitude == ((CheesyNote) obj).latitude
                    && longitude == ((CheesyNote) obj).longitude
                    && content == ((CheesyNote) obj).content);
        }
    }

    public void addNote(double latitude, double longitude, String content) {
        noteList.add(new CheesyNote(latitude, longitude, content));
    }

    public void removeNote(double latitude, double longitude, String content) {
        noteList.remove(new CheesyNote(latitude, longitude, content));
    }

    public List<CheesyNote> getNoteList() {
        return noteList;
    }

}
