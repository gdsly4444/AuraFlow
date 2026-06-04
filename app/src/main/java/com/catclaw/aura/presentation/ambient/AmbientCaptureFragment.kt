package com.catclaw.aura.presentation.ambient

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.catclaw.aura.R
import com.catclaw.aura.data.ambient.capture.NotificationListenerAccess
import com.catclaw.aura.data.ambient.AmbientCapturePortImpl
import com.catclaw.aura.databinding.FragmentAmbientCaptureBinding
import com.catclaw.aura.domain.model.AmbientMoment
import com.catclaw.aura.presentation.base.BaseFragment
import com.catclaw.aura.presentation.util.ImmersiveInsets
import com.catclaw.aura.AuraApplication
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import java.io.File
import java.util.Locale
import kotlin.math.max

/**
 * One-tap ambient sampling: short video, audio (≤5s), now-playing probe, location + Mapbox preview.
 */
class AmbientCaptureFragment : BaseFragment(R.layout.fragment_ambient_capture) {

    private var _binding: FragmentAmbientCaptureBinding? = null
    private val binding get() = _binding!!

    private val capturePortImpl: AmbientCapturePortImpl
        get() = (requireActivity().application as AuraApplication).container.ambientCapturePortImpl

    private val viewModel: AmbientCaptureViewModel by viewModels {
        AmbientCaptureViewModel.Factory(
            (requireActivity().application as AuraApplication).container,
        )
    }
    private var locationMapView: MapView? = null
    private var audioMediaPlayer: MediaPlayer? = null
    private var lastAudioPlaybackUri: Uri? = null
    private val audioProgressHandler = Handler(Looper.getMainLooper())
    private val audioProgressRunnable = object : Runnable {
        override fun run() {
            val player = audioMediaPlayer ?: return
            if (!player.isPlaying) return
            binding.seekbarAudio.progress = player.currentPosition
            binding.textAudioTime.text = formatAudioTime(
                player.currentPosition,
                player.duration.coerceAtLeast(0),
            )
            audioProgressHandler.postDelayed(this, 200L)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (requiredPermissions().all { grants[it] == true }) {
            startCapture()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.ambient_permission_denied,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onBind(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAmbientCaptureBinding.bind(view)
        ImmersiveInsets.applyPadding(binding.ambientScroll, basePaddingDp = 16)
        binding.buttonCapture.setOnClickListener { onCaptureClicked() }
        binding.buttonEnableNotificationAccess.setOnClickListener {
            startActivity(NotificationListenerAccess.settingsIntent())
        }
        updateNotificationAccessUi()

        viewModel.uiState.collectWithLifecycle { state ->
            binding.progressCapturing.isVisible = state.isCapturing
            binding.cameraPreview.isVisible = state.isCapturing
            binding.buttonCapture.isEnabled = !state.isCapturing
            if (state.isCapturing) {
                stopAudioPlayback()
            }
            binding.textError.isVisible = state.errorMessage != null
            binding.textError.text = state.errorMessage

            state.moment?.let { renderMoment(it) }
            renderWorkflowStatus(state)
        }

        viewModel.uiEvent.collectWithLifecycle { event ->
            when (event) {
                is AmbientCaptureUiEvent.ShowMessage -> {
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onCaptureClicked() {
        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions())
            return
        }
        startCapture()
    }

    private fun startCapture() {
        capturePortImpl.bindCaptureSession(viewLifecycleOwner, binding.cameraPreview)
        viewModel.capture()
    }

    private fun renderMoment(moment: AmbientMoment) {
        renderVideo(moment)
        renderAudio(moment)
        renderMusic(moment)
        renderLocation(moment)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun renderVideo(moment: AmbientMoment) {
        val video = moment.video
        if (video.isSuccess && video.uri != null) {
            binding.textVideoStatus.text = getString(
                R.string.ambient_video_ok,
                video.durationMs,
                video.uri.toString(),
            )
            binding.layoutVideoMotion.isVisible = true
            binding.textVideoPlayHint.isVisible = true
            video.posterUri?.let { binding.imageVideoPoster.setImageURI(Uri.parse(it)) }
            binding.videoPreview.isVisible = false
            binding.videoPreview.setVideoURI(toPlaybackUri(Uri.parse(video.uri!!)))
            binding.videoPreview.setOnPreparedListener { player ->
                player.isLooping = false
                player.setVolume(0f, 0f)
            }
            binding.videoPreview.setOnCompletionListener {
                stopMotionClipPreview()
            }
            binding.imageVideoPoster.setOnLongClickListener {
                startMotionClipPreview()
                true
            }
            binding.imageVideoPoster.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP ||
                    event.action == MotionEvent.ACTION_CANCEL
                ) {
                    stopMotionClipPreview()
                }
                false
            }
        } else {
            stopMotionClipPreview()
            binding.layoutVideoMotion.isVisible = false
            binding.textVideoPlayHint.isVisible = false
            binding.textVideoStatus.text = getString(
                R.string.ambient_video_failed,
                video.errorMessage ?: "unknown",
            )
        }
    }

    private fun startMotionClipPreview() {
        binding.imageVideoPoster.isVisible = false
        binding.videoPreview.isVisible = true
        binding.videoPreview.seekTo(0)
        binding.videoPreview.start()
    }

    private fun stopMotionClipPreview() {
        binding.videoPreview.pause()
        binding.videoPreview.seekTo(0)
        binding.videoPreview.isVisible = false
        binding.imageVideoPoster.isVisible = true
    }

    private fun renderAudio(moment: AmbientMoment) {
        val audio = moment.audio
        if (audio.isSuccess && audio.uri != null) {
            binding.textAudioStatus.text = getString(
                R.string.ambient_audio_ok,
                audio.durationMs,
                audio.uri.toString(),
            )
            binding.layoutAudioPlayback.isVisible = true
            setupAudioPlayback(Uri.parse(audio.uri!!), audio.durationMs)
        } else {
            binding.layoutAudioPlayback.isVisible = false
            releaseAudioPlayer()
            binding.textAudioStatus.text = getString(
                R.string.ambient_audio_failed,
                audio.errorMessage ?: "unknown",
            )
        }
    }

    private fun setupAudioPlayback(sourceUri: Uri, durationMs: Long) {
        val playbackUri = toPlaybackUri(sourceUri)
        val duration = max(durationMs.toInt(), 1)
        if (playbackUri == lastAudioPlaybackUri && audioMediaPlayer != null) {
            binding.seekbarAudio.max = duration
            return
        }
        releaseAudioPlayer()
        lastAudioPlaybackUri = playbackUri
        binding.seekbarAudio.max = duration
        binding.seekbarAudio.progress = 0
        binding.textAudioTime.text = formatAudioTime(0, duration)
        binding.buttonAudioPlay.text = getString(R.string.ambient_audio_play)
        binding.buttonAudioPlay.setOnClickListener { toggleAudioPlayback(playbackUri) }
        binding.seekbarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.textAudioTime.text = formatAudioTime(progress, duration)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                audioMediaPlayer?.seekTo(seekBar?.progress ?: 0)
            }
        })
    }

    private fun toggleAudioPlayback(playbackUri: Uri) {
        val player = audioMediaPlayer
        if (player?.isPlaying == true) {
            pauseAudioPlayback()
            return
        }
        if (player == null) {
            audioMediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), playbackUri)
                prepare()
                setOnCompletionListener {
                    binding.seekbarAudio.progress = binding.seekbarAudio.max
                    binding.buttonAudioPlay.text = getString(R.string.ambient_audio_play)
                    audioProgressHandler.removeCallbacks(audioProgressRunnable)
                    binding.textAudioTime.text = formatAudioTime(
                        binding.seekbarAudio.max,
                        binding.seekbarAudio.max,
                    )
                }
            }
        }
        audioMediaPlayer?.start()
        binding.buttonAudioPlay.text = getString(R.string.ambient_audio_pause)
        audioProgressHandler.removeCallbacks(audioProgressRunnable)
        audioProgressHandler.post(audioProgressRunnable)
    }

    private fun pauseAudioPlayback() {
        audioMediaPlayer?.pause()
        binding.buttonAudioPlay.text = getString(R.string.ambient_audio_play)
        audioProgressHandler.removeCallbacks(audioProgressRunnable)
    }

    private fun stopAudioPlayback() {
        pauseAudioPlayback()
        binding.seekbarAudio.progress = 0
        audioMediaPlayer?.seekTo(0)
    }

    private fun releaseAudioPlayer() {
        audioProgressHandler.removeCallbacks(audioProgressRunnable)
        audioMediaPlayer?.release()
        audioMediaPlayer = null
        lastAudioPlaybackUri = null
    }

    private fun toPlaybackUri(uri: Uri): Uri {
        if (uri.scheme == "file") {
            val path = uri.path ?: return uri
            return FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                File(path),
            )
        }
        return uri
    }

    private fun formatAudioTime(positionMs: Int, durationMs: Int): String =
        getString(
            R.string.ambient_audio_time,
            formatMs(positionMs),
            formatMs(durationMs),
        )

    private fun formatMs(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    private fun renderMusic(moment: AmbientMoment) {
        val music = moment.nowPlaying
        val trackLine = buildString {
            if (!music.title.isNullOrBlank()) append(music.title)
            if (!music.artist.isNullOrBlank()) {
                if (isNotEmpty()) append(" — ")
                append(music.artist)
            }
            if (!music.album.isNullOrBlank()) {
                append(" (")
                append(music.album)
                append(")")
            }
        }
        binding.textMusicStatus.text = buildString {
            append(getString(R.string.ambient_music_active, music.isMusicActive))
            append("\n")
            append(music.statusMessage)
            if (trackLine.isNotBlank()) {
                append("\n")
                append(getString(R.string.ambient_music_track, trackLine))
            }
            if (!music.packageName.isNullOrBlank()) {
                append("\n")
                append(getString(R.string.ambient_music_package, music.packageName))
            }
        }
    }

    private fun renderWorkflowStatus(state: AmbientCaptureUiState) {
        val show = state.workflowSubmitted && state.workflowId != null
        binding.cardWorkflowStatus.isVisible = show
        if (!show) return
        binding.textWorkflowStatus.text = getString(
            R.string.ambient_workflow_status_message,
            state.workflowId!!.take(8),
        )
    }

    private fun renderLocation(moment: AmbientMoment) {
        val location = moment.location
        if (location == null || !location.isSuccess) {
            binding.textLocationStatus.text = getString(
                R.string.ambient_location_failed,
                location?.errorMessage ?: "unknown",
            )
            clearMapPreview()
            return
        }

        binding.textLocationStatus.text = when {
            !location.placeName.isNullOrBlank() -> {
                val typeLabel = location.placeFeatureType?.let { featureTypeLabel(it) }
                val placeLine = if (typeLabel != null) {
                    "$typeLabel · ${location.placeName}"
                } else {
                    location.placeName!!
                }
                getString(
                    R.string.ambient_location_with_place,
                    placeLine,
                    location.latitude,
                    location.longitude,
                    location.accuracyMeters ?: 0f,
                    location.provider ?: "mapbox",
                )
            }
            !location.geocodingError.isNullOrBlank() -> getString(
                R.string.ambient_location_ok,
                location.latitude,
                location.longitude,
                location.accuracyMeters ?: 0f,
                location.provider ?: "mapbox",
            ) + "\n" + getString(R.string.ambient_geocoding_failed, location.geocodingError)
            else -> getString(
                R.string.ambient_location_ok,
                location.latitude,
                location.longitude,
                location.accuracyMeters ?: 0f,
                location.provider ?: "mapbox",
            )
        }
        showLocationOnMap(location.latitude, location.longitude)
    }

    private fun showLocationOnMap(latitude: Double, longitude: Double) {
        val host = binding.mapPreviewHost
        if (locationMapView == null) {
            locationMapView = MapView(
                requireContext(),
                MapInitOptions(
                    context = requireContext(),
                    cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(longitude, latitude))
                        .zoom(15.0)
                        .pitch(0.0)
                        .bearing(0.0)
                        .build(),
                ),
            )
            host.addView(
                locationMapView,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        } else {
            locationMapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(longitude, latitude))
                    .zoom(15.0)
                    .build(),
            )
        }
        locationMapView?.onStart()
    }

    private fun clearMapPreview() {
        locationMapView?.onStop()
        locationMapView?.onDestroy()
        binding.mapPreviewHost.removeAllViews()
        locationMapView = null
    }

    private fun featureTypeLabel(featureType: String): String? = when (featureType) {
        "poi" -> getString(R.string.ambient_location_place_type_poi)
        "address" -> getString(R.string.ambient_location_place_type_address)
        "street" -> getString(R.string.ambient_location_place_type_street)
        else -> null
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    override fun onResume() {
        super.onResume()
        updateNotificationAccessUi()
    }

    private fun updateNotificationAccessUi() {
        val enabled = NotificationListenerAccess.isEnabled(requireContext())
        binding.buttonEnableNotificationAccess.isVisible = !enabled
        binding.textNotificationAccessHint.isVisible = !enabled
    }

    override fun onStart() {
        super.onStart()
        locationMapView?.onStart()
    }

    override fun onStop() {
        pauseAudioPlayback()
        locationMapView?.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        stopMotionClipPreview()
        binding.videoPreview.stopPlayback()
        releaseAudioPlayer()
        clearMapPreview()
        _binding = null
        super.onDestroyView()
    }
}
