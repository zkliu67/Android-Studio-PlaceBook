package com.raywenderlich.placebook.model

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raywenderlich.placebook.util.ImageUtils

// Tells the room this is a database entity
@Entity

data class Bookmark (
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = "",
    var notes: String = ""
) {

    // provides the public interface for saving an image for a Bookmark
    fun setImage(image: Bitmap, context: Context) {
        id?.let {
            ImageUtils.saveBitmapToFile(context, image,
                generateImageFilename(it))
        }
    }

    companion object {
        fun generateImageFilename(id: Long): String {
            return "bookmark$id.png"
        }
    }
}

