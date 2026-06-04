package com.catclaw.aura.ui.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * Applies window inset padding/margins for edge-to-edge layouts.
 */
object ImmersiveInsets {

    fun applyPadding(
        view: View,
        basePaddingDp: Int = 0,
        applyTop: Boolean = true,
        applyBottom: Boolean = true,
        applyHorizontal: Boolean = true,
    ) {
        val density = view.resources.displayMetrics.density
        val base = (basePaddingDp * density).toInt()
        val baseLeft = if (view.paddingLeft != 0) view.paddingLeft else base
        val baseTop = if (view.paddingTop != 0) view.paddingTop else base
        val baseRight = if (view.paddingRight != 0) view.paddingRight else base
        val baseBottom = if (view.paddingBottom != 0) view.paddingBottom else base

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                baseLeft + if (applyHorizontal) bars.left else 0,
                baseTop + if (applyTop) bars.top else 0,
                baseRight + if (applyHorizontal) bars.right else 0,
                baseBottom + if (applyBottom) bars.bottom else 0,
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun applyMargin(
        view: View,
        extraTopDp: Int = 0,
        extraBottomDp: Int = 0,
        extraHorizontalDp: Int = 0,
    ) {
        val density = view.resources.displayMetrics.density
        val extraTop = (extraTopDp * density).toInt()
        val extraBottom = (extraBottomDp * density).toInt()
        val extraHorizontal = (extraHorizontalDp * density).toInt()
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val baseTop = lp.topMargin
        val baseBottom = lp.bottomMargin
        val baseLeft = lp.leftMargin
        val baseRight = lp.rightMargin

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = baseTop + extraTop + bars.top
                bottomMargin = baseBottom + extraBottom + bars.bottom
                leftMargin = baseLeft + extraHorizontal + bars.left
                rightMargin = baseRight + extraHorizontal + bars.right
            }
            windowInsets
        }
        ViewCompat.requestApplyInsets(view)
    }
}
