package com.raywenderlich.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.BookmarkRepo
import com.raywenderlich.placebook.util.ImageUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Use the bookmark repo to retrieve the bookmark detail
// and format it to the detail activity
class BookmarkDetailsViewModel(application: Application):
    AndroidViewModel(application) {

    // Create a new instance of BookmarkRepo
    private var bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    // 1. Define a new data class to hold the info required for the view class
    data class BookmarkDetailsView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = "",
        var category: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var placeId: String? = null
    ) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(context,
                    Bookmark.generateImageFilename(it))
            }
            return null
        }

        fun setImage(context: Context, image: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(context, image, Bookmark.generateImageFilename(it))
            }
        }
    }

    fun getCategories(): List<String> {
        return bookmarkRepo.categories
    }

    // 4. Put all together
    // input: bookmark id
    // return a Live bookmarkdetailsview for ui layer
    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        if(bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }

        return bookmarkDetailsView
    }

    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        // Connecting the database in the background
        GlobalScope.launch {
            // convert the bookmarkview to bookmark
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            bookmark?.let {
                // update the bookmark
                bookmarkRepo.updateBookmark(it)
            }
        }
    }

    fun getCategoryResourceId(category: String): Int? {
        return bookmarkRepo.getCategoryResourceId(category)
    }

    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkDetailsView.id?.let {
                bookmarkRepo.getBookmark(it)
            }
            bookmark?.let {
                bookmarkRepo.deleteBookmark(it)
            }
        }
    }

    // 3. Converts a live database bookmark object to a Live bookmark view object
    private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark) {repoBookmark ->
            repoBookmark?.let { bookmarkToBookmarkView(repoBookmark) }
        }
    }

    // 2. Converts a Bookmark model to BookmarkDetailsView
    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            BookmarkDetailsView {
        return BookmarkDetailsView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes,
            bookmark.category,
            bookmark.latitude,
            bookmark.longitude,
            bookmark.placeId
        )
    }

    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView)
            :Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }

        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }

        return bookmark
    }

}