package com.catclaw.aura

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.catclaw.aura.ui.ambient.AmbientCaptureFragment
import com.catclaw.aura.ui.map.MapFragment

/**
 * Single-activity host. Fragment navigation is handled by [replaceFragment] and screen helpers below.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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

    companion object {
        const val TAG_MAP = "map"
        const val TAG_AMBIENT = "ambient_capture"
    }
}
