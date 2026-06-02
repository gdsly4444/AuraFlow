package com.catclaw.aura

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //init map view
        mapView = MapView(
            this, MapInitOptions(
                context = this,
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(-98.0, 39.5))
                    .pitch(0.0)
                    .zoom(2.0)
                    .bearing(0.0)
                    .build()
            )
        )
        setContentView(mapView)
    }
}