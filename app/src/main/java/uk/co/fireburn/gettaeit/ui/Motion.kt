package uk.co.fireburn.gettaeit.ui

import android.content.Context
import android.provider.Settings

/** True when the person has chosen "Remove animations" in Android's accessibility settings. */
fun Context.prefersReducedMotion(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
