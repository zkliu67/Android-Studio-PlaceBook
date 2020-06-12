package com.raywenderlich.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.BookmarkRepo
import com.raywenderlich.placebook.util.ImageUtils

class MapsViewModel(application: Application):
        AndroidViewModel(application) {

    data class BookmarkMarkerView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0),
        var name: String = "",
        var phone: String = ""
    ) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(context,
                    Bookmark.generateImageFilename(it))
            }
            return null
        }
    }

    private val TAG = "MapsViewModel"
    // Create the bookmarkRepo object
    private var bookmarkRepo: BookmarkRepo = BookmarkRepo(
        getApplication() // provided by the base AndroidViewModel class.
    )
    private var bookmarks: LiveData<List<BookmarkMarkerView>>? = null

    fun getBookmarkMarkerViews():
            LiveData<List<BookmarkMarkerView>>? {
        if (bookmarks == null) {
            mapBookmarksToMarkerView()
        }
        return bookmarks
    }

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude?: 0.0
        bookmark.latitude = place.latLng?.latitude?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()

        val newId = bookmarkRepo.addBookmark(bookmark)

        image?.let { bookmark.setImage(it, getApplication()) }
        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    private fun mapBookmarksToMarkerView() {
        // Transformations class is a Lifecycle pacakge that transforms
        // value on the LiveData object before returned to the observer.
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks) {
            repoBookmarks ->
                repoBookmarks.map { bookmark ->
                    bookmarkToMarkerView(bookmark)
                }
        }
    }

    private fun bookmarkToMarkerView(bookmark: Bookmark):
            BookmarkMarkerView {
        return BookmarkMarkerView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone
        )
    }

}