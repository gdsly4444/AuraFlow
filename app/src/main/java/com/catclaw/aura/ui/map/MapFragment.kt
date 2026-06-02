package com.catclaw.aura.ui.map

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import com.catclaw.aura.R
import com.catclaw.aura.ui.base.BaseFragment
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView

/**
 * Map screen. [MapViewModel] supplies camera config; [MapView] lifecycle is tied to this fragment.
 */
class MapFragment : BaseFragment(R.layout.fragment_map) {

    private val viewModel: MapViewModel by viewModels()
    private var mapView: MapView? = null

    override fun onBind(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            viewModel.uiState.collectWithLifecycle { state ->
                if (mapView == null) {
                    mapView = createMapView(state)
                    view.findViewById<FrameLayout>(R.id.map_container).addView(
                        mapView,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    viewModel.onMapReady()
                }
            }
        } else if (mapView == null) {
            // MapView must be recreated after process death; state is restored via ViewModel defaults for now.
            val state = viewModel.uiState.value
            mapView = createMapView(state)
            view.findViewById<FrameLayout>(R.id.map_container).addView(
                mapView,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroyView() {
        mapView?.onDestroy()
        mapView = null
        super.onDestroyView()
    }

    private fun createMapView(state: MapUiState): MapView {
        return MapView(
            requireContext(),
            MapInitOptions(
                context = requireContext(),
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(state.centerLongitude, state.centerLatitude))
                    .pitch(state.pitch)
                    .zoom(state.zoom)
                    .bearing(state.bearing)
                    .build(),
            ),
        )
    }
}
