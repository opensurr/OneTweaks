package com.oneuitweak.app

import java.util.Locale

data class Overlay(val pkg: String, val enabled: Boolean)

data class Current(
    val anim: Float = 1f,
    val font: Float = 1f,
    val autoRotate: Boolean = false,
    val timeoutSec: Int = 60
)

object Tweaks {

    private val packageRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    private fun f(v: Float): String = String.format(Locale.US, "%.2f", v)
    private fun bit(on: Boolean): Int = if (on) 1 else 0

    fun setAnimationScale(scale: Float) = ShizukuShell.exec(
        "settings put global window_animation_scale ${f(scale)}",
        "settings put global transition_animation_scale ${f(scale)}",
        "settings put global animator_duration_scale ${f(scale)}"
    )

    fun setFontScale(scale: Float) = ShizukuShell.exec("settings put system font_scale ${f(scale)}")

    fun setDensity(dpi: Int?): ShizukuShell.Result =
        if (dpi == null || dpi !in 120..800) ShizukuShell.Result(false, "Enter a DPI between 120 and 800")
        else ShizukuShell.exec("wm density $dpi")

    fun resetDensity() = ShizukuShell.exec("wm density reset")

    fun setNightMode(mode: String): ShizukuShell.Result =
        if (mode !in setOf("yes", "no", "auto")) ShizukuShell.Result(false, "Invalid mode")
        else ShizukuShell.exec("cmd uimode night $mode")

    /** Sets the allowed range. Use the same number for min and max to lock the rate. */
    fun setRefreshRate(minHz: Int, maxHz: Int) = ShizukuShell.exec(
        "settings put system min_refresh_rate $minHz",
        "settings put system peak_refresh_rate $maxHz"
    )

    fun setAutoRotate(on: Boolean) = ShizukuShell.exec("settings put system accelerometer_rotation ${bit(on)}")

    fun setScreenTimeout(seconds: Int) = ShizukuShell.exec("settings put system screen_off_timeout ${seconds * 1000}")

    fun setBrightness(level: Int) = ShizukuShell.exec(
        "settings put system screen_brightness_mode 0",
        "settings put system screen_brightness ${level.coerceIn(0, 255)}"
    )

    fun setShowTouches(on: Boolean) = ShizukuShell.exec("settings put system show_touches ${bit(on)}")

    fun setPointerLocation(on: Boolean) = ShizukuShell.exec("settings put system pointer_location ${bit(on)}")

    fun setStayAwake(on: Boolean) = ShizukuShell.exec("settings put global stay_on_while_plugged_in ${if (on) 3 else 0}")

    fun setBatterySaver(on: Boolean) = ShizukuShell.exec("settings put global low_power ${bit(on)}")

    fun setWifi(on: Boolean) = ShizukuShell.exec("svc wifi ${if (on) "enable" else "disable"}")

    fun setBluetooth(on: Boolean) = ShizukuShell.exec("svc bluetooth ${if (on) "enable" else "disable"}")

    fun setMobileData(on: Boolean) = ShizukuShell.exec("svc data ${if (on) "enable" else "disable"}")

    fun setAppEnabled(pkg: String, enabled: Boolean): ShizukuShell.Result {
        if (!packageRegex.matches(pkg)) return ShizukuShell.Result(false, "Invalid package name")
        val cmd = if (enabled) "pm enable --user 0 $pkg" else "pm disable-user --user 0 $pkg"
        return ShizukuShell.exec(cmd)
    }

    fun readCurrent(): Current = Current(
        anim = ShizukuShell.query("global", "window_animation_scale")?.toFloatOrNull() ?: 1f,
        font = ShizukuShell.query("system", "font_scale")?.toFloatOrNull() ?: 1f,
        autoRotate = ShizukuShell.query("system", "accelerometer_rotation") == "1",
        timeoutSec = ((ShizukuShell.query("system", "screen_off_timeout")?.toLongOrNull() ?: 60000L) / 1000).toInt()
    )

    /** Lists theme overlays (RRO packs) installed on the phone. Parses `cmd overlay list`. */
    fun listOverlays(): List<Overlay> {
        val r = ShizukuShell.exec("cmd overlay list")
        return Regex("""\[( |x)\]\s+(\S+)""").findAll(r.output)
            .map { m -> Overlay(m.groupValues[2], m.groupValues[1] == "x") }
            .distinctBy { it.pkg }
            .toList()
    }

    fun setOverlay(pkg: String, on: Boolean): ShizukuShell.Result {
        if (!packageRegex.matches(pkg)) return ShizukuShell.Result(false, "Invalid package name")
        return ShizukuShell.exec("cmd overlay ${if (on) "enable" else "disable"} --user 0 $pkg")
    }

    /** Hides status bar icons by name. Empty list restores them. */
    fun setHiddenStatusIcons(icons: List<String>): ShizukuShell.Result =
        if (icons.isEmpty()) ShizukuShell.exec("settings delete secure icon_blacklist")
        else ShizukuShell.exec("settings put secure icon_blacklist ${icons.joinToString(",")}")

    fun applyProfile(p: Profile): ShizukuShell.Result {
        val results = listOf(
            setAnimationScale(p.anim),
            setFontScale(p.font),
            setNightMode(p.nightMode),
            setAutoRotate(p.autoRotate),
            setScreenTimeout(p.timeoutSec)
        )
        return results.firstOrNull { !it.ok } ?: ShizukuShell.Result(true, "Profile ${p.name} applied")
    }

    fun resetAll(): ShizukuShell.Result {
        val results = listOf(
            setAnimationScale(1f),
            setFontScale(1f),
            resetDensity(),
            setNightMode("auto"),
            setRefreshRate(60, 120),
            setAutoRotate(false),
            setScreenTimeout(60),
            setShowTouches(false),
            setPointerLocation(false),
            setStayAwake(false),
            setBatterySaver(false),
            setHiddenStatusIcons(emptyList())
        )
        return results.firstOrNull { !it.ok } ?: ShizukuShell.Result(true, "Reset done")
    }
}
