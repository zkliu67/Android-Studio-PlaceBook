package com.raywenderlich.placebook.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.util.ImageUtils
import com.raywenderlich.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_detail.*
import kotlinx.android.synthetic.main.content_bookmark_info.*
import java.io.File
import java.io.FilePermission


class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {

    private lateinit var bookmarkDetailsViewModel: BookmarkDetailsViewModel
    private var bookmarkDetailsView:
            BookmarkDetailsViewModel.BookmarkDetailsView? = null
    // Hold a reference to the temporary image file when capturing an image.
    private var photoFile: File? = null

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
    // get called when an Activity returns a result
    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAPTURE_IMAGE -> {
                    val photoFile = photoFile ?: return
                    val uri = FileProvider.getUriForFile(this,
                        "com.raywenderlich.placebook.fileprovider",
                        photoFile)
                    revokeUriPermission(uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val image = getImageWithPath(photoFile.absolutePath)
                    image?.let { updateImage(it) }
                }
                REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null) {
                    val imageUri = data.data
                    val image = imageUri?.let { getImageWithAuthority(it) }
                    image?.let { updateImage(it) }
                }
            }
        }
    }

    override fun opCaptureClick() {
        photoFile = null // Clear any previously assigned file
        try {
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: java.io.IOException) {
            return
        }

        photoFile?.let {photoFile ->
            // get a Uri for temporary photo file
            // Fileprovider should be registered in the Manifest.xml
            val photoUri = FileProvider.getUriForFile(this,
                "com.raywenderlich.placebook.fileprovider",
                photoFile)
            // Allows to display the camera viewfinder
            // and snap a new photo
            val captureIntent =
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                photoUri) // let intent knows where to save the full-sized image.

            // give temporary write permission on the photoUri to Intent
            val intentActivities = packageManager.queryIntentActivities(
                captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
            intentActivities.map { it.activityInfo.packageName }
                .forEach{ grantUriPermission(it, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }

            startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
        }

    }
    // Display image selection activity
    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
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
                    populateCategoryList()
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
            imageViewPlace.setOnClickListener { replaceImage() }
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
            bookmarkView.category = spinnerCategory.selectedItem as String
        }
        finish() // Close the activity
    }

    // When the user taps the bookmark image, call replaceImage and display
    // the dialog fragment.
    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }
    // Update the edited bookmark with new image
    private fun updateImage(image: Bitmap) {
        val bookmarkView = bookmarkDetailsView ?: return
        imageViewPlace.setImageBitmap(image)
        bookmarkView.setImage(this, image)
    }

    private fun getImageWithPath(filePath: String): Bitmap? {
        return ImageUtils.decodeFileToSize(filePath,
            resources.getDimensionPixelSize(R.dimen.default_image_width),
            resources.getDimensionPixelSize(R.dimen.default_image_height))
    }

    private fun getImageWithAuthority(uri: Uri): Bitmap? {
        return ImageUtils.decodeUriStreamToSize(uri,
            resources.getDimensionPixelSize(R.dimen.default_image_width),
            resources.getDimensionPixelSize(R.dimen.default_image_height),this)
    }

    private fun populateCategoryList() {
        val bookmarkView = bookmarkDetailsView ?: return
        // retrieve the category icon from view model.
        val resourceId =
            bookmarkDetailsViewModel.getCategoryResourceId(bookmarkView.category)
        // If icon exists, display icon in the imageView
        resourceId?.let { imageViewCategory.setImageResource(it) }
        // return a list of all categories
        val categories = bookmarkDetailsViewModel.getCategories()
        // a standard way to populate a spinner control.
        // 1. create an adapter, which is ArrayAdapter in this case
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, categories)
        // 2. assign the adapter to a built-in layout resource
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        // assign the adapter to the category control
        spinnerCategory.adapter = adapter
        val placeCategory = bookmarkView.category
        // update the selection to reflect the current category control
        spinnerCategory.setSelection(adapter.getPosition(placeCategory))

        // after selecting the new category
        // 1. update the icon display; 2. save the current category
        // Setup a listener to respond when the user changes the category selection
        spinnerCategory.post {
            // assign the spinnerCategory.onItemSelectedListener to an instance
            // of the onItemSelectedListener class that implements onItemSelected and onNothing.
            spinnerCategory.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View,
                                            position: Int, id: Long) {
                    // determine the new category by the current spinner selection position
                    val category = parent.getItemAtPosition(position) as String
                    val resourceId =
                        bookmarkDetailsViewModel.getCategoryResourceId(category)
                    // update the image view
                    resourceId?.let {
                        imageViewCategory.setImageResource(it) }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    // NOTE: This method is required but not used.
                }
            }
        }
    }

    companion object {
        // The request code to use when processing the camera capture intent.
        private const val REQUEST_CAPTURE_IMAGE = 1
        private const val REQUEST_GALLERY_IMAGE = 2
    }

}