package com.raywenderlich.placebook.Adapter

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.ui.MapsActivity
import com.raywenderlich.placebook.viewmodel.MapsViewModel

// a custom InfoWindowAdapter
class BookmarkInfoWindowAdapter(val context: Activity):
    GoogleMap.InfoWindowAdapter {

    private val contents: View = context.layoutInflater.inflate(
        R.layout.content_bookmark_info, null
    )

    // Allows to return a custom view for the interior contents
    // without changing the default outer window and background.
    override fun getInfoContents(marker: Marker): View {
        val titleView = contents.findViewById(R.id.title) as TextView
        titleView.text = marker.title?: ""

        val phoneView = contents.findViewById(R.id.phone) as TextView
        phoneView.text = marker.snippet?: ""

        val imageView = contents.findViewById(R.id.photo) as ImageView
        //imageView.setImageBitmap((marker.tag as MapsActivity.PlaceInfo).image)
        when (marker.tag) {

            // If marker.tag is MapsActivity.PlaceInfo, you set the imageView bitmap
            // directly from the PlaceInfo.image
            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap(
                    (marker.tag as MapsActivity.PlaceInfo).image)
            }

            // If marker.tag is a MapsViewModel.BookmarkMarkerView, you set the imageView
            // bitmap from the BookmarkMarkerView.
            is MapsViewModel.BookmarkView -> {
                var bookMarkView = marker.tag as
                        MapsViewModel.BookmarkView
                // Set imageView bitmap here...
                imageView.setImageBitmap(bookMarkView.getImage(context))
            }
        }
        return contents
    }

    // Allows to return a custom view for the full window
    override fun getInfoWindow(p0: Marker?): View? {
        // This function is required, but can return null
        // if not replacing the entire info window
        return null
    }
}