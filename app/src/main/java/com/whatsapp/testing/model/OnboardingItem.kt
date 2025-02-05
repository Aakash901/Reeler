package com.whatsapp.testing.model

import androidx.annotation.DrawableRes

// OnboardingItem.kt
data class OnboardingItem(
    @DrawableRes val image: Int,
    val title: String,
    val description: String
)