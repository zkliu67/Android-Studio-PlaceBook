package com.raywenderlich.placebook.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

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

    // Returns an empty file in the app's private folder using a unique filename
    // Potentially throw IOException when createTempFile.
    @Throws(IOException::class)
    fun createUniqueImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val filename = "PlaceBook_" + timeStamp + "_"
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename, ".jpg", filesDir)
    }

    fun decodeFileToSize(filePath: String, width: Int, height: Int): Bitmap {
        // Load the size of image using BitmapFactory.decodeFile()
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true // just load the size not the actual file
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight, width, height)

        options.inJustDecodeBounds = false // load the full image
        return BitmapFactory.decodeFile(filePath, options) // return the downsampled file
    }

    fun decodeUriStreamToSize(
        uri: Uri, width: Int, height: Int, context: Context): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options: BitmapFactory.Options
            // Input stream is opened for the uri
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // read the image size from stream uri
                options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                BitmapFactory.decodeStream(inputStream, null, options)
                // the stream is closed and reopen again for null check
                inputStream.close()
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // the image is then loaded from the stream using the downsampling
                    // options and returned to the caller
                    options.inSampleSize = calculateInSampleSize(
                        options.outWidth, options.outHeight, width, height)
                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()
                    return bitmap
                }
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
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

    // Downsample the captured image to match the default bookmark photo size
    // Calculate the optimum inSampleSize that can be used to resize an image to
    // a specific width and height.
    private fun calculateInSampleSize(
        width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                    halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}