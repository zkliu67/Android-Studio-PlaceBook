package com.raywenderlich.placebook.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
        setupViewModel()
        getIntentData()
    }

    // Provide the item to the tool bar, by loading the menu layout.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    // method called when the user selects a toolbar checkmark item
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                saveChanges()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    // Get called when the activity is created
    private fun getIntentData() {
        val activity = MapsActivity.EXTRA_BOOKMARK_ID
        // Pull the bookmark id from the intent
        val bookmarkId = intent.getLongExtra(
            MapsActivity.EXTRA_BOOKMARK_ID, 0
        )

        // Retrieve the BookmarkDetailsView from the ModelView and observe for it changes
        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
            this, Observer {
                // Whenever the bookmark is loaded or changed
                // Assign it to current bookmark
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

    private fun saveChanges() {
        val name = editTextName.text.toString()
        if (name.isEmpty()) {
            return
        }

        bookmarkDetailsView?.let { bookmarkView ->
            bookmarkView.name = editTextName.text.toString()
            bookmarkView.notes = editTextNote.text.toString()
            bookmarkView.address = editTextAddress.text.toString()
            bookmarkView.phone = editTextPhone.text.toString()
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish() // Close the activity
    }

}