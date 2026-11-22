package com.fincip.nativeapp.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Launcher icon colour variants. Each one is an <activity-alias> in the manifest; exactly one is enabled. */
data class AppIconOption(val id: String, val label: String, val alias: String, val bg: Long, val leaf: Long, val shoot: Long)

object AppIcons {
    val options = listOf(
        AppIconOption("classic", "Classic", ".IconClassic", 0xFFF6F5EF, 0xFF171717, 0xFFB9F227),
        AppIconOption("forest", "Forest", ".IconForest", 0xFF1F5C3A, 0xFFF6F5EF, 0xFFB9F227),
        AppIconOption("midnight", "Midnight", ".IconMidnight", 0xFF121212, 0xFFF6F5EF, 0xFFB9F227),
        AppIconOption("sunrise", "Sunrise", ".IconSunrise", 0xFFF59E3B, 0xFF171717, 0xFFFFFFFF)
    )

    private fun component(c: Context, alias: String) = ComponentName(c.packageName, c.packageName + alias)

    fun current(c: Context): AppIconOption {
        val pm = c.packageManager
        return options.firstOrNull { pm.getComponentEnabledSetting(component(c, it.alias)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
            ?: options.first()
    }

    fun apply(c: Context, option: AppIconOption) {
        val pm = c.packageManager
        // Enable the new alias first so there is never a moment with no launcher entry.
        pm.setComponentEnabledSetting(component(c, option.alias), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        options.filter { it.id != option.id }.forEach {
            pm.setComponentEnabledSetting(component(c, it.alias), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
