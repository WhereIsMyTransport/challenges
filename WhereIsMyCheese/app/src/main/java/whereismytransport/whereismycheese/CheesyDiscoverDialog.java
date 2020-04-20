package whereismytransport.whereismycheese;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Show cheesy note
 */
public class CheesyDiscoverDialog extends Dialog implements View.OnClickListener {

    public Activity context;
    private String content;
    public Button saveButton, exitButton;
    public IDiscoverDialogListener listener;

    public CheesyDiscoverDialog(Activity context, String content, IDiscoverDialogListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_discover_note);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ((TextView) findViewById(R.id.noteText)).setText(content);
        saveButton = (Button) findViewById(R.id.discoverCheeseButton);
        exitButton = (Button) findViewById(R.id.exitDialogButton);
        saveButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.discoverCheeseButton:
                listener.onNoteDiscovered();
                dismiss();
                break;
            case R.id.exitDialogButton:
                dismiss();
                break;
            default:
                break;
        }
    }

    public interface IDiscoverDialogListener {
        public void onNoteDiscovered();
    }
}
