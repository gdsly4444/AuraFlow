package com.catclaw.aura

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.catclaw.aura.ui.ambient.AmbientCaptureFragment
import com.catclaw.aura.ui.map.MapFragment
import com.catclaw.aura.ui.moment.MomentDetailFragment

/**
 * Single-activity host. Fragment navigation is handled by [replaceFragment] and screen helpers below.
 */
class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional; foreground still works when denied on some OEMs */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        requestNotificationPermissionIfNeeded()
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            showMapFragment()
        }
    }

    /** Shows the map screen (start destination). */
    fun showMapFragment(addToBackStack: Boolean = false) {
        replaceFragment(MapFragment(), TAG_MAP, addToBackStack)
    }

    /** Shows ambient capture (video + audio + music + location). */
    fun showAmbientCaptureFragment(addToBackStack: Boolean = true) {
        replaceFragment(AmbientCaptureFragment(), TAG_AMBIENT, addToBackStack)
    }

    /** Full-screen rounded moment card detail. */
    fun showMomentDetailFragment(cardId: String, addToBackStack: Boolean = true) {
        replaceFragment(
            MomentDetailFragment.newInstance(cardId),
            "$TAG_MOMENT_DETAIL:$cardId",
            addToBackStack,
        )
    }

    /**
     * Replaces the content of [R.id.fragment_container] with [fragment].
     *
     * @param tag Used for [androidx.fragment.app.FragmentManager.findFragmentByTag].
     * @param addToBackStack When true, the transaction is added to the back stack.
     */
    fun replaceFragment(
        fragment: Fragment,
        tag: String,
        addToBackStack: Boolean = false,
    ) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, tag)
            if (addToBackStack) {
                addToBackStack(tag)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val TAG_MAP = "map"
        const val TAG_AMBIENT = "ambient_capture"
        const val TAG_MOMENT_DETAIL = "moment_detail"
    }
}
