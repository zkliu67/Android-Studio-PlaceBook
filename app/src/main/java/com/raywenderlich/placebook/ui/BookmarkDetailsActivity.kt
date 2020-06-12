package com.raywenderlich.placebook.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.db.BookmarkDao
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_detail.*


class BookmarkDetailsActivity : AppCompatActivity() {

    private lateinit var bookmarkDetailsViewModel: BookmarkDetailsViewModel
    private var bookmarkDetailsView:
            BookmarkDetailsViewModel.BookmarkDetailsView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_detail)
        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(
            MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0
        )
        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
            this, Observer {
                it?.let {
                   bookmarkDetailsView  = it
                    populateFields()
                    populateImageView()
                }
            }
        )
    }

    // Initialize the view model
    private fun setupViewModel() {
        bookmarkDetailsViewModel = ViewModelProviders.of(this).get(
            BookmarkDetailsViewModel::class.java
        )
    }
    // Populates all the UI fields using the current bookmarkView if not null.
    private fun populateFields() {
        bookmarkDetailsView?.let { bookmarkView ->
            editTextName.setText(bookmarkView.name)
            editTextPhone.setText(bookmarkView.phone)
            editTextNote.setText(bookmarkView.notes)
            editTextAddress.setText(bookmarkView.address)
        }
    }
    // Populate place image to imageView
    private fun populateImageView() {
        bookmarkDetailsView?.let {bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let {
                imageViewPlace.setImageBitmap(placeImage)
            }
        }
    }


}