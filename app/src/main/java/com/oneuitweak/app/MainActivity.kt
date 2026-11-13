package com.oneuitweak.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.app.WallpaperManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val SHIZUKU_DOWNLOAD = "https://shizuku.rikka.app/download/"

class MainActivity : ComponentActivity() {

    private val connected = mutableStateOf(false)
    private val oneUi = mutableStateOf(OneUiCheck.Info(false, "Checking..."))
    private val current = mutableStateOf(Current())
    private val profiles = mutableStateOf<List<Profile>>(emptyList())
    private val busy = mutableStateOf(false)
    private val message = mutableStateOf("Welcome to OneUI Tweak. Connect Shizuku to unlock tweaks.")
    private val notifGranted = mutableStateOf(false)
    private val a11yEnabled = mutableStateOf(false)
    private val wallpapers = mutableStateOf<List<WallpaperApp>>(emptyList())
    private val overlays = mutableStateOf<List<Overlay>>(emptyList())
    private val requestCode = 1001

    private val onBinderReceived = Shizuku.OnBinderReceivedListener { runOnUiThread { refresh() } }
    private val onBinderDead = Shizuku.OnBinderDeadListener { runOnUiThread { refresh() } }
    private val onPermission = Shizuku.OnRequestPermissionResultListener { _, result ->
        runOnUiThread {
            if (result == PackageManager.PERMISSION_GRANTED) {
                message.value = "Connected to Shizuku. Tweaks are ready."
                notifyConnected()
            } else {
                message.value = "Shizuku permission was denied."
            }
            refresh()
        }
    }

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted.value = granted
        message.value = if (granted) "Notifications allowed" else "Notifications blocked"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermission)

        profiles.value = ProfileStore.load(this)
        wallpapers.value = loadLiveWallpapers()

        lifecycleScope.launch(Dispatchers.IO) {
            val info = OneUiCheck.check()
            withContext(Dispatchers.Main) { oneUi.value = info }
        }

        setContent {
            OneUiTweakTheme {
                AppRoot(
                    state = UiState(
                        connected = connected.value,
                        oneUi = oneUi.value,
                        current = current.value,
                        profiles = profiles.value,
                        busy = busy.value,
                        message = message.value,
                        notifGranted = notifGranted.value,
                        a11yEnabled = a11yEnabled.value,
                        wallpapers = wallpapers.value,
                        overlays = overlays.value
                    ),
                    actions = Actions(
                        tweak = this::runTweak,
                        connect = this::connectShizuku,
                        openShizuku = this::openShizuku,
                        requestNotifications = this::requestNotifications,
                        openAccessibility = this::openAccessibility,
                        refresh = this::refresh,
                        setLauncher = { variant ->
                            setLauncherIcon(this@MainActivity, variant)
                            message.value = "Icon changed. The launcher may take a moment."
                        },
                        saveProfile = this::saveProfile,
                        deleteProfile = this::deleteProfile,
                        setLiveWallpaper = this::setLiveWallpaper,
                        openWallpaperPicker = this::openWallpaperPicker,
                        openFontSettings = this::openFontSettings,
                        loadOverlays = this::loadOverlays,
                        toggleOverlay = this::toggleOverlay
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(onBinderReceived)
        Shizuku.removeBinderDeadListener(onBinderDead)
        Shizuku.removeRequestPermissionResultListener(onPermission)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        connected.value = ShizukuShell.isReady && ShizukuShell.hasPermission
        notifGranted.value = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        a11yEnabled.value = isAccessibilityEnabled()
        if (connected.value) {
            lifecycleScope.launch(Dispatchers.IO) {
                val c = Tweaks.readCurrent()
                withContext(Dispatchers.Main) { current.value = c }
            }
        }
    }

    private fun runTweak(label: String, block: () -> ShizukuShell.Result) {
        if (busy.value) return
        busy.value = true
        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { block() }
                message.value = if (r.ok) "$label: done" else "$label failed: ${r.output.ifBlank { "unknown error" }}"
            } finally {
                busy.value = false
                refresh()
            }
        }
    }

    private fun connectShizuku() {
        when {
            !ShizukuShell.isReady -> {
                message.value = "Shizuku is not running. Start it, then tap Connect again."
                openShizuku()
            }
            ShizukuShell.hasPermission -> {
                message.value = "Already connected to Shizuku"
                refresh()
            }
            else -> runCatching { Shizuku.requestPermission(requestCode) }
                .onFailure { message.value = "Could not request permission: ${it.message}" }
        }
    }

    private fun openShizuku() {
        val intent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse(SHIZUKU_DOWNLOAD))
        runCatching { startActivity(intent) }.onFailure { message.value = "Could not open Shizuku" }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifGranted.value = true
        }
    }

    private fun openAccessibility() {
        message.value = "Find OneUI Tweak in the list and turn it on."
        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains("$packageName/")
    }

    private fun loadLiveWallpapers(): List<WallpaperApp> =
        packageManager.queryIntentServices(Intent("android.service.wallpaper.WallpaperService"), 0)
            .map { ri ->
                WallpaperApp(
                    label = ri.loadLabel(packageManager).toString(),
                    pkg = ri.serviceInfo.packageName,
                    cls = ri.serviceInfo.name
                )
            }
            .sortedBy { it.label }

    private fun setLiveWallpaper(w: WallpaperApp) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(w.pkg, w.cls)
        )
        runCatching { startActivity(intent) }
            .onSuccess { message.value = "Confirm the wallpaper on the next screen." }
            .onFailure { message.value = "Could not open the live wallpaper preview." }
    }

    private fun openWallpaperPicker() {
        runCatching { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
            .onFailure { message.value = "Could not open the wallpaper picker." }
    }

    private fun loadOverlays() {
        if (!ShizukuShell.isReady || !ShizukuShell.hasPermission) {
            message.value = "Connect Shizuku first."
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val list = Tweaks.listOverlays()
            withContext(Dispatchers.Main) {
                overlays.value = list
                message.value = if (list.isEmpty()) "No theme overlays found." else "Found ${list.size} overlays."
            }
        }
    }

    private fun toggleOverlay(pkg: String, on: Boolean) {
        overlays.value = overlays.value.map { if (it.pkg == pkg) it.copy(enabled = on) else it }
        runTweak("Overlay ${pkg.substringAfterLast('.')}") { Tweaks.setOverlay(pkg, on) }
    }

    private fun openFontSettings() {
        val galaxyThemes = packageManager.getLaunchIntentForPackage("com.samsung.android.themestore")
        val intent = galaxyThemes ?: Intent(Settings.ACTION_DISPLAY_SETTINGS)
        runCatching { startActivity(intent) }
            .onFailure { message.value = "Could not open font settings" }
    }

    @SuppressLint("MissingPermission")
    private fun notifyConnected() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "shizuku_status"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(channelId, "Connection status", NotificationManager.IMPORTANCE_LOW))

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("OneUI Tweak connected")
            .setContentText("Shizuku is ready. You can change tweaks now.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(1, notification)
    }

    private fun saveProfile(p: Profile) {
        val updated = profiles.value.filterNot { it.name == p.name } + p
        profiles.value = updated
        ProfileStore.save(this, updated)
        message.value = "Saved profile ${p.name}"
    }

    private fun deleteProfile(name: String) {
        val updated = profiles.value.filterNot { it.name == name }
        profiles.value = updated
        ProfileStore.save(this, updated)
        message.value = "Deleted profile $name"
    }

}

/** Switches only this app's own launcher icon. Needs no Shizuku. */
fun setLauncherIcon(context: Context, variant: String) {
    val pm = context.packageManager
    listOf("LauncherDefault", "LauncherDark").forEach { name ->
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, "${context.packageName}.$name"),
            if (name == variant) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
