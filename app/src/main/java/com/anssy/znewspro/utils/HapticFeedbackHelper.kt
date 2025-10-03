package com.anssy.znewspro.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Helper class for providing haptic feedback throughout the app
 * Supports both system haptic feedback and custom vibration patterns
 */
object HapticFeedbackHelper {
    
    /**
     * Provides light haptic feedback for navigation interactions
     * Uses system haptic feedback for better integration with user preferences
     */
    fun performNavigationHaptic(view: View) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } else {
                @Suppress("DEPRECATION")
                view.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        } catch (e: Exception) {
            // Fallback to light vibration if haptic feedback fails
            performLightVibration(view.context)
        }
    }
    
    /**
     * Provides medium haptic feedback for important actions
     */
    fun performMediumHaptic(view: View) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } else {
                @Suppress("DEPRECATION")
                view.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        } catch (e: Exception) {
            // Fallback to medium vibration
            performMediumVibration(view.context)
        }
    }
    
    /**
     * Provides light vibration as fallback
     */
    private fun performLightVibration(context: Context) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            // Ignore if vibration is not available
        }
    }
    
    /**
     * Provides medium vibration as fallback
     */
    private fun performMediumVibration(context: Context) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            // Ignore if vibration is not available
        }
    }
    
    /**
     * Gets the vibrator instance based on Android version
     */
    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
