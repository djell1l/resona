package com.savoo.scclient.ui.util

import androidx.core.graphics.ColorUtils
import kotlin.random.Random

object ColorUtilsHelper {
    fun generatedArtworkSeedColor(seed: Any?): Int {
        val hue = Random(seed?.hashCode() ?: 0).nextFloat()
        return ColorUtils.HSLToColor(floatArrayOf(hue * 360f, 0.72f, 0.58f))
    }
}
