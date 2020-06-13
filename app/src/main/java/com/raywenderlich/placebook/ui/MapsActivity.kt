package com.raywenderlich.placebook.ui

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProviders
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
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.raywenderlich.placebook.Adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var mapsViewModel: MapsViewModel

    companion object {

        // Defines a key to store the bookmark id in the intent extras.
        const val EXTRA_BOOKMARK_ID =
            "com.raywenderlich.placebook.EXTRA_BOOKMARK_ID"
        // request code passed to requestPermission()
        // Used to identify the specific permission request when the request is returned by android.
        private const val REQUEST_LOCATION = 1

        // TAG is passed to Log.e method,
        // used to print information to the Logcat window for debugging.
        private const val TAG = "MapsActivity"
    }

    class PlaceInfo (val place: Place? = null, val image: Bitmap? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Loads the activity_maps.xml
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        // Initialize the map using getMapAsync()
        mapFragment.getMapAsync(this)

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
    }

    private fun setupViewModel() {
        mapsViewModel =
            ViewModelProviders.of(this).get(MapsViewModel::class.java)
        createBookmarkMarkerObserver()
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
            is MapsViewModel.BookmarkMarkerView -> {
                val bookmarkMarkerView = marker.tag as MapsViewModel.BookmarkMarkerView
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
                }
            }
    }

    private fun displayPoiGetPhotoStep(place: Place) {
        // Get the photoMetadata for the selected place.
        val photoMetadata = place.photoMetadatas?.get(0)

        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
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
    private fun createBookmarkMarkerObserver() {
        mapsViewModel.getBookmarkMarkerViews()?.observe(
            this, androidx.lifecycle
                .Observer {
                    mMap.clear()
                    it?.let {
                        displayAllBookmarks(it)
                    }
                }
        )
    }

    // Walk through all the book marks and add marker to each of them.
    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkMarkerView>) {
        for (bookmark in bookmarks) {
            addPlaceMarker(bookmark)
        }
    }

    // Add place marker for a single location
    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkMarkerView): Marker? {
        val marker = mMap.addMarker(MarkerOptions()
            .position(bookmark.location)
            .title(bookmark.name)
            .snippet(bookmark.phone)
            .icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE))
            .alpha(0.8f))
        marker.tag = bookmark
        return marker
    }

}