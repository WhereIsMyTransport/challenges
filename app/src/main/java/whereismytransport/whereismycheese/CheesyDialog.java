package whereismytransport.whereismycheese;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * We need some way to add a Cheezy Note. We will also require some way to show the user the cheezy note..
 * Feel free to just use AlertDialog if time is an issue.
 */
public class CheesyDialog extends Dialog implements View.OnClickListener {

    public Activity context;
    public Button saveButton, exitButton;
    public EditText noteEditText;
    public INoteDialogListener listener;

    public CheesyDialog(Activity context, INoteDialogListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_note);

        noteEditText = (EditText) findViewById(R.id.noteText);
        saveButton = (Button) findViewById(R.id.saveCheeseButton);
        exitButton = (Button) findViewById(R.id.exitDialogButton);
        saveButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.saveCheeseButton:
                String note = noteEditText.getText().toString();
                if (note.isEmpty()) {
                    return;
                }
                listener.onNoteAdded(note);
                dismiss();
                break;
            case R.id.exitDialogButton:
                dismiss();
                break;
            default:
                break;
        }
    }

    public interface INoteDialogListener {
        public void onNoteAdded(String note);
    }
}
