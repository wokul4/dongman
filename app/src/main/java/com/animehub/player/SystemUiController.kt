package com.animehub.player

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object SystemUiController {

    fun enterImmersive(activity: Activity) {
        activity.window?.let { window ->
            val controller = WindowInsetsControllerCompat(window, window.decorView)

            if (Build.VERSION.SDK_INT >= 35) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.hide(WindowInsetsCompat.Type.displayCutout())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun exitImmersive(activity: Activity) {
        activity.window?.let { window ->
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun setDecorFitsSystemWindows(activity: Activity, fits: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, fits)
    }
}
