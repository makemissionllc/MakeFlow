package com.makemission.makeflow.core.haptics

enum class HapticStrength(
    val storageValue: String,
    val amplitudeScale: Float,
    val durationScale: Float
) {
    OFF("off", 0f, 0f),
    SOFT("soft", 0.45f, 0.8f),
    STANDARD("standard", 1f, 1f),
    STRONG("strong", 1.5f, 1.2f);

    companion object {
        fun fromStorage(value: String?): HapticStrength =
            entries.firstOrNull { it.storageValue == value } ?: STANDARD
    }
}
