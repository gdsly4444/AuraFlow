package com.catclaw.aura.ui.moment

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.catclaw.aura.R
import com.catclaw.aura.data.moment.model.MomentCard
import com.catclaw.aura.databinding.FragmentMomentDetailBinding
import com.catclaw.aura.ui.base.BaseFragment
import com.catclaw.aura.ui.util.ImmersiveInsets
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MomentDetailFragment : BaseFragment(R.layout.fragment_moment_detail) {

    private var _binding: FragmentMomentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MomentDetailViewModel by viewModels {
        MomentDetailViewModelFactory(
            requireActivity().application,
            requireArguments().getString(ARG_CARD_ID)!!,
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

    override fun onBind(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMomentDetailBinding.bind(view)
        ImmersiveInsets.applyPadding(binding.cardRoot, basePaddingDp = 12)
        viewModel.card.collectWithLifecycle { card ->
            card?.let { renderCard(it) }
        }
    }

    private fun renderCard(card: MomentCard) {
        binding.textHeroMeta.text = buildHeroMeta(card)
        card.posterPath?.let { binding.imageHero.setImageURI(Uri.fromFile(File(it))) }

        val description = card.sceneDescription
        binding.textSceneDescription.isVisible = !description.isNullOrBlank()
        binding.textSceneDescription.text = description
        binding.textSceneError.isVisible = !card.sceneDescriptionError.isNullOrBlank()
        binding.textSceneError.text = card.sceneDescriptionError

        renderVideo(card)
        renderAudio(card)
        renderMusic(card)
        renderLocation(card)
    }

    private fun buildHeroMeta(card: MomentCard): String {
        val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(card.createdAtEpochMs))
        val loc = if (card.latitude != null && card.longitude != null) {
            getString(
                R.string.moment_list_location_short,
                card.latitude,
                card.longitude,
            )
        } else {
            getString(R.string.moment_detail_location_unknown)
        }
        return "$time · $loc"
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun renderVideo(card: MomentCard) {
        val hasVideo = !card.videoPath.isNullOrBlank()
        binding.layoutVideoMotion.isVisible = hasVideo
        binding.textVideoHint.isVisible = hasVideo
        if (!hasVideo) return

        card.posterPath?.let { binding.imageVideoPoster.setImageURI(Uri.fromFile(File(it))) }
        val videoUri = toPlaybackUri(Uri.fromFile(File(card.videoPath!!)))
        binding.videoPreview.setVideoURI(videoUri)
        binding.videoPreview.setOnPreparedListener { player ->
            player.isLooping = false
            player.setVolume(0f, 0f)
        }
        binding.videoPreview.setOnCompletionListener { stopMotionClipPreview() }
        binding.imageVideoPoster.setOnLongClickListener {
            startMotionClipPreview()
            true
        }
        binding.imageVideoPoster.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                stopMotionClipPreview()
            }
            false
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

    private fun renderAudio(card: MomentCard) {
        val path = card.audioPath
        if (path.isNullOrBlank()) {
            binding.layoutAudioPlayback.isVisible = false
            return
        }
        binding.layoutAudioPlayback.isVisible = true
        val playbackUri = toPlaybackUri(Uri.fromFile(File(path)))
        val duration = max(card.audioDurationMs.toInt(), 1)
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
                if (fromUser) binding.textAudioTime.text = formatAudioTime(progress, duration)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                audioMediaPlayer?.seekTo(seekBar?.progress ?: 0)
            }
        })
    }

    private fun toggleAudioPlayback(playbackUri: Uri) {
        if (audioMediaPlayer?.isPlaying == true) {
            pauseAudioPlayback()
            return
        }
        if (audioMediaPlayer == null) {
            audioMediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), playbackUri)
                prepare()
                setOnCompletionListener {
                    binding.seekbarAudio.progress = binding.seekbarAudio.max
                    binding.buttonAudioPlay.text = getString(R.string.ambient_audio_play)
                    audioProgressHandler.removeCallbacks(audioProgressRunnable)
                }
            }
        }
        audioMediaPlayer?.start()
        binding.buttonAudioPlay.text = getString(R.string.ambient_audio_pause)
        audioProgressHandler.post(audioProgressRunnable)
    }

    private fun pauseAudioPlayback() {
        audioMediaPlayer?.pause()
        binding.buttonAudioPlay.text = getString(R.string.ambient_audio_play)
        audioProgressHandler.removeCallbacks(audioProgressRunnable)
    }

    private fun releaseAudioPlayer() {
        audioProgressHandler.removeCallbacks(audioProgressRunnable)
        audioMediaPlayer?.release()
        audioMediaPlayer = null
        lastAudioPlaybackUri = null
    }

    private fun renderMusic(card: MomentCard) {
        val trackLine = buildString {
            if (!card.musicTitle.isNullOrBlank()) append(card.musicTitle)
            if (!card.musicArtist.isNullOrBlank()) {
                if (isNotEmpty()) append(" — ")
                append(card.musicArtist)
            }
            if (!card.musicAlbum.isNullOrBlank()) {
                append(" (")
                append(card.musicAlbum)
                append(")")
            }
        }
        binding.textMusicStatus.text = buildString {
            append(getString(R.string.ambient_music_active, card.musicActive))
            append("\n")
            append(card.musicStatusMessage)
            if (trackLine.isNotBlank()) {
                append("\n")
                append(getString(R.string.ambient_music_track, trackLine))
            }
            if (!card.musicPackageName.isNullOrBlank()) {
                append("\n")
                append(getString(R.string.ambient_music_package, card.musicPackageName))
            }
        }
    }

    private fun renderLocation(card: MomentCard) {
        if (card.latitude == null || card.longitude == null) {
            binding.textLocationStatus.text = getString(R.string.moment_detail_location_unknown)
            clearMapPreview()
            return
        }
        binding.textLocationStatus.text = getString(
            R.string.ambient_location_ok,
            card.latitude,
            card.longitude,
            card.locationAccuracyMeters ?: 0f,
            card.locationProvider ?: "mapbox",
        )
        showLocationOnMap(card.latitude, card.longitude)
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

    companion object {
        const val ARG_CARD_ID = MomentDetailViewModel.CARD_ID_ARG

        fun newInstance(cardId: String): MomentDetailFragment =
            MomentDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CARD_ID, cardId)
                }
            }
    }
}
