package com.raywenderlich.placebook.ui

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ApiException

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.raywenderlich.placebook.Adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.Adapter.BookmarkListAdapter
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_detail.*
import kotlinx.android.synthetic.main.activity_bookmark_detail.toolbar
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer_view_maps.*
import kotlinx.android.synthetic.main.main_view_maps.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var mapsViewModel: MapsViewModel
    private lateinit var bookmarkListAdapter: BookmarkListAdapter

    private val MSG = "MapsActivityMsg"

    // Manage all the markers for bookmark in a HashMap.
    // Map a bookmark ID to a Marker.
    private var markers = HashMap<Long, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Loads the activity_maps.xml
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        // Initialize the map using getMapAsync()
        mapFragment.getMapAsync(this)

        setupToolbar()
        setupNavigationDrawer()

        setupLocationClient()
        setupPlacesClient() // Create the client when starting the app.
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapListeners() // set adapter and PoiClickListener
        setupViewModel() // Initiate view model
        getCurrentLocation() // To get current location

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    val location = Location("")

                    location.latitude = place.latLng?.latitude?: 0.0
                    location.longitude = place.latLng?. longitude?: 0.0

                    updateMapToLocation(location)
                    showProgress() // show progress bar after searching for a place before the place is loaded.
                    displayPoiGetPhotoStep(place)
                }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        // takes drawerlayout and toogle and fully manage the
        // display and functionality of the toggle icon.
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.open_drawer ,R.string.close_drawer
        )
        // ensures the toggle icon is displayed initially.
        toggle.syncState()
    }

    // Set up the adapter for the bookmark recycler view.
    private fun setupNavigationDrawer() {
        // gets the recyclerView from the layout and sets a default LinearLayoutManager
        val layoutManager = LinearLayoutManager(this)
        bookmarkRecyclerView.layoutManager = layoutManager
        // Creates a new BookmarkListAdapter and assigns it to the RecyclerView.
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    private fun setupMapListeners() {
        mMap.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))

        // mMap object will call the lambda anytime it detects the user taps on a POI.
        // Lambda passes a single params of type of PointOfInterest, with 3 variables
        // 1. latLng: geometry coordinate
        // 2. name: name of POI.
        // 3. placeId: id, a unique identifier.
        // Doing this may display a toast of place name.
        mMap.setOnPoiClickListener {
            displayPoi(it)
        }
        // Set up a listener to call handleInfoWindowClick() whenever
        // the user taps an info window.
        mMap.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }

        fab.setOnClickListener {
            searchAtCurrentLocation()
        }
        // manually add a new location by long press the map location.
        mMap.setOnMapLongClickListener { latLng ->
            newBookmark(latLng)
        }
    }

    private fun showProgress() {
        progressBar.visibility = ProgressBar.VISIBLE
        disableUserInteraction()
    }

    private fun hideProgress() {
        progressBar.visibility = ProgressBar.GONE
        enableUserInteraction()
    }

    private fun setupViewModel() {
        mapsViewModel =
            ViewModelProviders.of(this).get(MapsViewModel::class.java)
        createBookmarkObserver()
    }

    private fun setupPlacesClient() {
        // Creates the PlacesClient
        Places.initialize(applicationContext, "AIzaSyAEq7q3hUiOY1di9OKexg2VjBHBISDZjxU");
        placesClient = Places.createClient(this)
    }

    private fun setupLocationClient() {
        fusedLocationClient =
            LocationServices.
            getFusedLocationProviderClient(this)
    }

    // Add run-time permission.
    // Use requestPermissions() to prompt the user to grant or deny the
    // ACCESS_FINE_LOCATION permission.
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION
        )
    }

    private fun handleInfoWindowClick(marker: Marker) {
        // handles taps on a place info window.
        when (marker.tag) {
            // If the place is never be liked (red marker)
            // Click the marker to like it
            is PlaceInfo -> {
                val placeInfo = marker.tag as PlaceInfo
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
                    }
                }
                marker.remove()
            }

            // If the place is liked (blue marker)
            // Click the marker to view/add detail
            is MapsViewModel.BookmarkView -> {
                val bookmarkMarkerView = marker.tag as MapsViewModel.BookmarkView
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let {
                    startBookmarkDetails(it)
                }
            }
        }
    }

    // Start the BookmarkDetailActivity with explicit Intent
    // Called when the user taps on an info window.
    private fun startBookmarkDetails(bookmarkId: Long) {
        // Adds the bookmark ID as an extra parameter on the Intent
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    // Time to get user's current location
    // And move the map to center the location.
    private fun getCurrentLocation() {

        // Check if the permission is granted before requesting the location.
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {

            requestLocationPermission()
        } else {
            /*
                isMyLocationEnabled adds several features
                1. displays a trusty blue dot that keeps up with user's current location
                2. displays a target icon for recenter the map for user's location
                3. add controls to rotate the view and bearing
                4. handles everything to location updates.
             */
            mMap.isMyLocationEnabled = true

            // lastLocation() is a Task that runs in the background to check the location
            // addOnCompleteListener is called when the location is ready
            fusedLocationClient.lastLocation.addOnCompleteListener {
                // returns a Location object containing the last known location.
                // It can be null when no location data available
                val location = it.result
                if (location != null) {
                    // Create a LatLng object for recording coordinate.
                    val latLng = LatLng(location.latitude, location.longitude)
                    // create a CameraUpdate object, tells how the map camera is updated.
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    // Update Camera
                    mMap.moveCamera(update)
                } else {
                    Log.e(TAG, "No location found.")
                }
            }
        }
    }

    private fun displayPoi(pointOfInterest: PointOfInterest) {
        showProgress() // progress visible and disables user interaction
        displayPoiGetPlaceStep(pointOfInterest)
    }

    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val placeId = pointOfInterest.placeId
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        // use placeId and needed fields to fetch request.
        val request = FetchPlaceRequest
            .builder(placeId, placeFields)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                displayPoiGetPhotoStep(place) // get location image
                /*
                Toast.makeText(
                    this,
                    "${place.name}, " +
                            "${place.phoneNumber}",
                    Toast.LENGTH_LONG
                ).show()

                 */
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG,
                        "Place not found " +
                                exception.message + ", " +
                                "statusCode: " + statusCode
                    )
                    hideProgress()
                }
            }
    }

    private fun displayPoiGetPhotoStep(place: Place) {
        hideProgress()
        // Get the photoMetadata for the selected place.
        val photoMetadata = place.photoMetadatas?.get(0)

        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }

        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata as PhotoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(
                R.dimen.default_image_width
            ))
            .setMaxHeight(resources.getDimensionPixelSize(
                R.dimen.default_image_height
            ))
            .build()

        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                val bitmap = fetchPhotoResponse.bitmap
                displayPoiDisplayStep(place, bitmap)

            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG,
                        "Place not found " +
                                exception.message + ", " +
                                "statusCode: " + statusCode
                    )
                }
            }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {

        // returns a Marker object and assign it to marker.
        val marker = mMap.addMarker(MarkerOptions()
            .position(place.latLng as LatLng) // locate the location
            .title(place.name) // by tapping a marker, display a standard info window
            .snippet(place.phoneNumber)) // with title and snippet.

        // assign the photo as a tag
        marker?.tag = PlaceInfo(place, photo)
        marker?.showInfoWindow()
    }

    // observes the changes to BookmarkMarkerView
    // And updates the view when they change.
    // Also updates the recycler view adapter.
    private fun createBookmarkObserver() {
        mapsViewModel.getBookmarkViews()?.observe(
            this, androidx.lifecycle
                .Observer {
                    mMap.clear()
                    markers.clear() // clears the marker when the bookmark changes
                    it?.let {
                        displayAllBookmarks(it)
                        bookmarkListAdapter.setBookmarkData(it)
                    }
                }
        )
    }

    // Walk through all the book marks and add marker to each of them.
    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkView>) {
        for (bookmark in bookmarks) {
            addPlaceMarker(bookmark)
        }
    }

    // Add place marker for a single location
    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkView): Marker? {
        val marker = mMap.addMarker(MarkerOptions()
            .position(bookmark.location)
            .title(bookmark.name)
            .snippet(bookmark.phone)
                // Setting an icon corresponding to the category
            .icon(bookmark.categoryResourceId?.let {
                BitmapDescriptorFactory.fromResource(it)
            })
            .alpha(0.8f))
        marker.tag = bookmark
        bookmark.id?.let {
            markers.put(it, marker)
        }
        return marker
    }

    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
        drawerLayout.closeDrawer(drawerView)
        val marker = markers[bookmark.id]
        marker?.showInfoWindow()
        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapToLocation(location)
    }

    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        // Pans and zooms the map to the center of the location
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
    }

    // Use autocomplete for search functionality
    private fun searchAtCurrentLocation() {
        // defines the fields informing the Autocomplete widget what attributes to return.
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.TYPES
        )
        // Compute the bounds of the current visible region of the map.
        val bounds = RectangularBounds.newInstance(
            mMap.projection.visibleRegion.latLngBounds)
        try {
            /*
            Autocomplete.IntentBuilder provides an intent.
            - AutocompleteActivityMode.OVERLAY to display search widget as overlay
            - AutocompleteActivityMode.FULLSCREEN cause the search interface
            to replace the entire screen.
             */
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, placeFields)
                .setLocationBias(bounds) // apply the bounds.
                .build(this)
            // start the activity with a request_code.
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: GooglePlayServicesNotAvailableException) {
            // TODO: Handle exception
        } catch (e: GooglePlayServicesRepairableException) {
            // TODO: Handle exception
        }
    }

    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let { startBookmarkDetails(it) }
        }
    }

    private fun disableUserInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun enableUserInteraction() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    companion object {

        // Defines a key to store the bookmark id in the intent extras.
        const val EXTRA_BOOKMARK_ID =
            "com.raywenderlich.placebook.EXTRA_BOOKMARK_ID"
        // request code passed to requestPermission()
        // Used to identify the specific permission request when the request is returned by android.
        private const val REQUEST_LOCATION = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 2

        // TAG is passed to Log.e method,
        // used to print information to the Logcat window for debugging.
        private const val TAG = "MapsActivity"
    }

    class PlaceInfo (val place: Place? = null, val image: Bitmap? = null)
}