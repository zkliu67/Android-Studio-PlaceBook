package com.raywenderlich.placebook.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.DialogFragment

// Display a dialog for user choosing existing image or capturing a new one.
/*
Defines a dialog fragment that shows an AlertDialog based on device capabilities.
1. Select image from gallery; 2. Capture new image from camera.
 */
class PhotoOptionDialogFragment: DialogFragment() {

    // Defines an interface must be implemented by the parent activity.
    // In this case is BookmarkDetailsActivity
    interface PhotoOptionDialogListener {
        fun opCaptureClick()
        fun onPickClick()
    }

    // A property to hold an instance of PhotoOptionDialogListener
    private lateinit var listener: PhotoOptionDialogListener

    // A standard method for DialogFragment.
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // listener is set to parent Activity
        listener = activity as PhotoOptionDialogListener

        var captureSelectIdx = -1
        var pickSelectIdx = -1

        val options = ArrayList<String>() // For holding the dialog options

        // Set the array options
        val context = activity as Context
        if (canCapture(context)) {
            options.add("Camera")
            captureSelectIdx = 0
        }

        if (canPick(context)) {
            options.add("Gallery")
            pickSelectIdx = if(captureSelectIdx == 0) 1 else 0
        }

        return AlertDialog.Builder(context)
            .setTitle("Photo Option")
            .setItems(options.toTypedArray()) {
                _, which -> // "which" refers to the click call
                if (which == captureSelectIdx) {
                    listener.opCaptureClick()
                } else if (which == pickSelectIdx) {
                    listener.onPickClick()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {
        // Determines whether the device can pick images from gallery
        fun canPick(context: Context): Boolean {
            // Create an intent for picking images
            val pickIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            // Checks if the Intent can be resolved.
            return (pickIntent.resolveActivity(
                context.packageManager) != null)
        }

        fun canCapture(context: Context): Boolean {
            val captureIntent = Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
            )

            return (captureIntent.resolveActivity(
                context.packageManager)!=null)
        }

        // a helper method intended to be used by the parent activity
        // when creating a new DialogFragment.
        fun newInstance(context: Context):
                PhotoOptionDialogFragment? {
            if (canPick(context) || canCapture(context)) {
                return PhotoOptionDialogFragment()
            } else {
                return null
            }
        }
    }
}