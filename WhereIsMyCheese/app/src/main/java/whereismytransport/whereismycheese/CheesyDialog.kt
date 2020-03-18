package whereismytransport.whereismycheese

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText

class CheesyDialog(context: Activity, var listener: INoteDialogListener) : Dialog(context), View.OnClickListener {

    private lateinit var noteEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_note)
        noteEditText = findViewById(R.id.noteText)
        findViewById<View>(R.id.saveCheeseButton).setOnClickListener(this)
        findViewById<View>(R.id.exitDialogButton).setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.saveCheeseButton -> {
                listener.onNoteAdded(noteEditText.text.toString())
                dismiss()
            }
            R.id.exitDialogButton -> dismiss()
            else -> {
            }
        }
    }

    interface INoteDialogListener {
        fun onNoteAdded(note: String)
    }
}