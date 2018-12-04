package com.cameronmacleod.coinz

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.zip.Inflater

class SendToFriendDialog : DialogFragment(), View.OnClickListener {
    private lateinit var mListener: NoticeDialogListener
    private lateinit var dialogView: View

    interface NoticeDialogListener {
        fun onUserIDGottenClick(uid: String)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = targetFragment as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((targetFragment.toString() +
                    " must implement NoticeDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            dialogView = activity!!.layoutInflater.inflate(R.layout.fragment_send_friend_dialog, null)
            dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener(this)
            dialogView.findViewById<Button>(R.id.send_button).setOnClickListener(this)

            builder.setTitle(R.string.send_friend_title)
                    .setView(dialogView)
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.send_button -> {
                getUserIDForEmail(
                        dialogView.findViewById<EditText>(R.id.friend_email_edit)
                                .text.toString()) { uid ->
                    if (uid == null) {
                        val toast = Toast.makeText(activity,
                                R.string.no_friend_found, Toast.LENGTH_SHORT)
                        toast.show()
                    } else {
                        dialog.dismiss()
                        mListener.onUserIDGottenClick(uid)
                    }
                }
            }
            R.id.cancel_button -> dialog.cancel()
        }
    }
}