package com.raywenderlich.placebook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

// ImageUtils is declared as an object, it behaves as a singleton.
object ImageUtils {

    // Load Image from file
    fun loadBitmapFromFile(context: Context, filename: String):
        Bitmap? {
        val filePath = File(context.filesDir, filename).absolutePath
        return BitmapFactory.decodeFile(filePath)
    }

    // save the Bitmap to permanent storage.
    fun saveBitmapToFile(context: Context, bitmap: Bitmap,
                         filename:String) {
        // create a stream to hold the image data
        val stream = ByteArrayOutputStream()

        // write bitmap to stream object using PNG format.
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        // Convert stream to an array of bytes.
        val bytes = stream.toByteArray()

        saveBytesToFile(context, bytes, filename)
    }

    // write the bytes to a file.
    private fun saveBytesToFile(context: Context, byte: ByteArray, filename: String){
        val outputStream: FileOutputStream
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            outputStream.write(byte)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}