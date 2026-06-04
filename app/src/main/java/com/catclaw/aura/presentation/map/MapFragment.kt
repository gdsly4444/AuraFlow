package com.catclaw.aura.presentation.map

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.catclaw.aura.MainActivity
import com.catclaw.aura.R
import com.catclaw.aura.databinding.FragmentMapBinding
import com.catclaw.aura.domain.model.HomeListEntry
import com.catclaw.aura.presentation.base.BaseFragment
import com.catclaw.aura.presentation.moment.MomentCardListAdapter
import com.catclaw.aura.presentation.moment.label
import com.catclaw.aura.AuraApplication
import com.catclaw.aura.di.AppContainer
import com.catclaw.aura.presentation.util.ImmersiveInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView

/**
 * Home: globe map + draggable moment list bottom sheet.
 *
 * MapView is attached on the next frame so the first layout stays light.
 */
class MapFragment : BaseFragment(R.layout.fragment_map) {

    private val appContainer: AppContainer
        get() = (requireActivity().application as AuraApplication).container

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val mapViewModel: MapViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(appContainer)
    }

    private var mapView: MapView? = null
    private var mapAttachScheduled = false
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private lateinit var listAdapter: MomentCardListAdapter

    override fun onBind(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMapBinding.bind(view)
        ImmersiveInsets.applyMargin(binding.layoutHomeGenerating, extraTopDp = 8)
        ImmersiveInsets.applyMargin(binding.fabAmbient, extraBottomDp = 16, extraHorizontalDp = 16)
        ImmersiveInsets.applyMargin(binding.buttonExpandMoments, extraTopDp = 8)
        setupBottomSheet()
        setupMomentList()
        setupGeneratingBanner()
        binding.fabAmbient.setOnClickListener {
            (requireActivity() as MainActivity).showAmbientCaptureFragment(addToBackStack = true)
        }
        binding.buttonExpandMoments.setOnClickListener { expandMomentSheet() }
        binding.momentSheetHeader.setOnClickListener { expandMomentSheet() }
        binding.momentSheetDragHandle.setOnClickListener { expandMomentSheet() }

        scheduleMapAttach()
    }

    private fun scheduleMapAttach() {
        if (mapView != null || mapAttachScheduled) return
        mapAttachScheduled = true
        binding.mapContainer.post {
            mapAttachScheduled = false
            if (!isAdded || _binding == null || mapView != null) return@post
            attachMapView()
        }
    }

    private fun attachMapView() {
        val state = mapViewModel.uiState.value
        mapView = createMapView(state).also { map ->
            binding.mapContainer.addView(
                map,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        mapViewModel.onMapReady()
        if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                androidx.lifecycle.Lifecycle.State.STARTED,
            )
        ) {
            mapView?.onStart()
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.momentBottomSheet).apply {
            isFitToContents = false
            halfExpandedRatio = 0.5f
            skipCollapsed = false
            isHideable = false
            peekHeight = resources.getDimensionPixelSize(R.dimen.moment_sheet_peek_height)
            // Collapsed first — avoids heavy half-expanded measure on cold start.
            state = BottomSheetBehavior.STATE_COLLAPSED
            addBottomSheetCallback(sheetCallback)
        }
        updateSheetUi(BottomSheetBehavior.STATE_COLLAPSED)
    }

    private fun expandMomentSheet() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    private val sheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            updateSheetUi(newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    private fun updateSheetUi(state: Int) {
        val collapsed = state == BottomSheetBehavior.STATE_COLLAPSED
        binding.momentSheetList.isVisible = !collapsed
        binding.buttonExpandMoments.isVisible = collapsed
    }

    private fun setupGeneratingBanner() {
        homeViewModel.generatingStatus.collectWithLifecycle { status ->
            val visible = status != null
            binding.layoutHomeGenerating.isVisible = visible
            if (status == null) return@collectWithLifecycle
            binding.textHomeGeneratingTitle.text = getString(
                R.string.home_generating_banner_title,
                status.activeCount,
            )
            binding.textHomeGeneratingPhase.text = getString(
                R.string.home_generating_banner_phase,
                status.primaryPhase.label(requireContext()),
            )
        }
    }

    private fun setupMomentList() {
        listAdapter = MomentCardListAdapter(
            onItemClick = { item ->
                when (item) {
                    is HomeListEntry.Completed -> {
                        (requireActivity() as MainActivity).showMomentDetailFragment(item.card.id)
                    }
                    is HomeListEntry.InProgress -> Unit
                }
            },
            onItemLongClick = { item -> onMomentItemLongClick(item) },
        )
        binding.recyclerMoments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        homeViewModel.listItems.collectWithLifecycle { items ->
            listAdapter.submitList(items)
            val inProgressCount = items.count { it is HomeListEntry.InProgress }
            binding.chipActiveCount.isVisible = inProgressCount > 0
            if (inProgressCount > 0) {
                binding.chipActiveCount.text = getString(
                    R.string.moment_list_active_count,
                    inProgressCount,
                )
            }
            val showEmpty = items.isEmpty()
            binding.textEmptyMoments.isVisible = showEmpty
            binding.recyclerMoments.isVisible = !showEmpty
        }
    }

    private fun onMomentItemLongClick(item: HomeListEntry) {
        when (item) {
            is HomeListEntry.InProgress -> {
                Toast.makeText(
                    requireContext(),
                    R.string.moment_delete_in_progress,
                    Toast.LENGTH_SHORT,
                ).show()
            }
            is HomeListEntry.Completed -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.moment_delete_confirm_title)
                    .setMessage(R.string.moment_delete_confirm_message)
                    .setNegativeButton(R.string.moment_delete_confirm_negative, null)
                    .setPositiveButton(R.string.moment_delete_confirm_positive) { _, _ ->
                        homeViewModel.deleteCard(item.card.id)
                        Toast.makeText(
                            requireContext(),
                            R.string.moment_delete_done,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    .show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        bottomSheetBehavior?.removeBottomSheetCallback(sheetCallback)
        mapView?.onDestroy()
        mapView = null
        mapAttachScheduled = false
        bottomSheetBehavior = null
        _binding = null
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
