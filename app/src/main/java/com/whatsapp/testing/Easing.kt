package com.whatsapp.testing

import android.view.animation.AccelerateDecelerateInterpolator

object Easing {
    val EaseOutCubic = object : AccelerateDecelerateInterpolator() {
        override fun getInterpolation(input: Float): Float {
            return 1f - (1f - input) * (1f - input) * (1f - input)
        }
    }
}