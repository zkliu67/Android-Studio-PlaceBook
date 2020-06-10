package com.raywenderlich.placebook

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest: LocationRequest? = null // check for periodic update.

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
        getCurrentLocation() // To get current location
        /*
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

         */
    }

    private fun setupLocationClient() {
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    // Add run-time permission.
    // Use requestPermissions() to prompt the user to grant or deny the
    // ACCESS_FINE_LOCATION permission.
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION)
    }

    companion object {
        // request code passed to requestPermission()
        // Used to identify the specific permission request when the request is returned by android.
        private const val REQUEST_LOCATION = 1

        // TAG is passed to Log.e method,
        // used to print information to the Logcat window for debugging.
        private const val TAG = "MapsActivity"
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
            // check periodic update
            // Check if the locationRequest is already been created.
            if (locationRequest == null) {
                // if not created, create a new one.
                locationRequest = LocationRequest.create()
                // If the creation succeed ...
                locationRequest?.let { locationRequest ->
                    // 1. Set priority.
                    locationRequest.priority =
                        LocationRequest.PRIORITY_HIGH_ACCURACY

                    // 2. Specify the interval to return the updates.
                    locationRequest.interval = 5000
                    // 3. The shortest interval the app is capable handling.
                    locationRequest.fastestInterval = 1000


                    val locationCallback = object: LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            // To grab the latest location and center the map.
                            getCurrentLocation()
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }

            // lastLocation() is a Task that runs in the background to check the location
            // addOnCompleteListener is called when the location is ready
            fusedLocationClient.lastLocation.addOnCompleteListener {
                // returns a Location object containing the last known location.
                // It can be null when no location data available
                val location = it.result
                if (location != null) {
                    // Create a LatLng object for recording coordinate.
                    val latLng = LatLng(location.latitude, location.longitude)
                    print("lat: ${location.latitude}, log: ${location.longitude}")
                    // addMarker() add and display the marker on the map.
                    mMap.clear()
                    
                    mMap.addMarker(MarkerOptions().position(latLng).title("You are here!"))
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
}