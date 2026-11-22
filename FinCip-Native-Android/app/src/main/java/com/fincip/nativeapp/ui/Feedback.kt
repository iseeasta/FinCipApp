package com.fincip.nativeapp.ui

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Global switches for tap sound and haptics (persisted by MainActivity in the "fincip_ui" prefs). */
object Feedback {
    var sound = true
    var haptic = true
}

/** Returns a function that plays the system click sound and a light haptic tick, honouring [Feedback]. */
@Composable
fun rememberTap(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            if (Feedback.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
            if (Feedback.haptic) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
