package com.savoo.scclient.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.savoo.scclient.data.repository.HapticsIntensity

fun Context.vibratorService(): Vibrator =
    if (Build.VERSION.SDK_INT >= 31) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

private fun HapticsIntensity.multiplier(): Float = when (this) {
    HapticsIntensity.LOW -> 0.6f
    HapticsIntensity.MEDIUM -> 1f
    HapticsIntensity.HIGH -> 1.45f
}

private enum class Tap { SOFT, LIGHT, MEDIUM, HEAVY }

private class Pulse(val tap: Tap, val scale: Float, val delayMs: Int = 0)

class Haptics(
    private val vibrator: Vibrator,
    private val enabled: Boolean = true,
    private val intensity: HapticsIntensity = HapticsIntensity.MEDIUM,
) {
    private val primitives: Boolean = Build.VERSION.SDK_INT >= 30 && vibrator.areAllPrimitivesSupported(
        VibrationEffect.Composition.PRIMITIVE_TICK,
        VibrationEffect.Composition.PRIMITIVE_CLICK,
    )
    private val lowTick: Boolean = primitives && Build.VERSION.SDK_INT >= 31 &&
        vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)

    fun tick() = play(Pulse(Tap.SOFT, 0.6f))
    fun click() = play(Pulse(Tap.LIGHT, 0.6f))
    fun longPress() = play(Pulse(Tap.MEDIUM, 0.55f))

    private fun play(pulse: Pulse) {
        if (!enabled) return
        val gain = intensity.multiplier()
        if (gain <= 0f) return

        if (primitives) {
            val (primitive, scale) = when (pulse.tap) {
                Tap.SOFT -> (if (lowTick) VibrationEffect.Composition.PRIMITIVE_LOW_TICK else VibrationEffect.Composition.PRIMITIVE_TICK) to 0.45f
                Tap.LIGHT -> VibrationEffect.Composition.PRIMITIVE_TICK to 0.7f
                Tap.MEDIUM -> VibrationEffect.Composition.PRIMITIVE_CLICK to 0.75f
                Tap.HEAVY -> VibrationEffect.Composition.PRIMITIVE_CLICK to 1f
            }
            val finalScale = (scale * pulse.scale * gain).coerceIn(0.05f, 1f)
            val comp = VibrationEffect.startComposition().addPrimitive(primitive, finalScale, pulse.delayMs).compose()
            vibrator.vibrate(comp)
            return
        }

        val (length, base) = when (pulse.tap) {
            Tap.SOFT -> 4L to 70
            Tap.LIGHT -> 6L to 120
            Tap.MEDIUM -> 9L to 180
            Tap.HEAVY -> 12L to 230
        }
        val amplitude = (base * pulse.scale * gain).toInt().coerceIn(1, 255)
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(length, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(length)
        }
    }
}
