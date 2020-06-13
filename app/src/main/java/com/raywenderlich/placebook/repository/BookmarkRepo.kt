package com.raywenderlich.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.raywenderlich.placebook.db.BookmarkDao
import com.raywenderlich.placebook.db.PlaceBookDatabase
import com.raywenderlich.placebook.model.Bookmark

// Context object is required to get an instance of the BookmarkDatabase class
class BookmarkRepo(context: Context) {

    val allBookmarks: LiveData<List<Bookmark>>
        get() {
            return bookmarkDao.loadAll()
        }
    // get the PlaceBookDatabase singleton instance
    private var db = PlaceBookDatabase.getInstance(context)
    // get DAO object from PlaceBookDatabase.
    private var bookmarkDao: BookmarkDao = db.bookmarkDao()

    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }

    fun createBookmark(): Bookmark {
        return Bookmark()
    }

    fun getLiveBookmark(bookmarkId: Long): LiveData<Bookmark> {
        return bookmarkDao.loadLiveBookmark(bookmarkId)
    }

    fun getBookmark(bookmarkId: Long): Bookmark {
        return bookmarkDao.loadBookmark(bookmarkId)
    }

    fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark)
    }

}