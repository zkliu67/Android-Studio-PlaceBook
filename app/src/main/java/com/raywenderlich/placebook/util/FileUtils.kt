package com.raywenderlich.placebook.util

import android.content.Context
import java.io.File

object FileUtils  {
    // a utility method that deletes a file in the app's main files directory
    // for deleting the image file associated with a delete bookmark.
    fun deleteFile(context: Context, filename: String) {
        val dir = context.filesDir
        val file = File(dir, filename)
        file.delete()
    }
}