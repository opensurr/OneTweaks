package com.oneuitweak.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable   // <-- FIX: added
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// ---------- Theme: white + brand blue, navy in dark mode ----------

private val LightColors = lightColorScheme(
    primary = Color(0xFF3468CB), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FB), onPrimaryContainer = Color(0xFF0B1E3F),
    secondary = Color(0xFF4A6FA5), onSecondary = Color.White,
    background = Color(0xFFF3F6FC), onBackground = Color(0xFF0B1E3F),
    surface = Color.White, onSurface = Color(0xFF0B1E3F),
    surfaceVariant = Color(0xFFE8EEF9), onSurfaceVariant = Color(0xFF4A5B7C),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB0FF), onPrimary = Color(0xFF0B1E3F),
    primaryContainer = Color(0xFF1B3A73), onPrimaryContainer = Color(0xFFDCE6FB),
    secondary = Color(0xFFA9C4F5), onSecondary = Color(0xFF0B1E3F),
    background = Color(0xFF0B1E3F), onBackground = Color.White,
    surface = Color(0xFF122A55), onSurface = Color.White,
    surfaceVariant = Color(0xFF1B3A73), onSurfaceVariant = Color(0xFFC9D8F5),
    error = Color(0xFFFFB4AB)
)

private val GoodGreen = Color(0xFF2E9E5B)

@Composable
fun OneUiTweakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

// ---------- State and actions ----------

data class WallpaperApp(val label: String, val pkg: String, val cls: String)

data class UiState(
    val connected: Boolean,
    val oneUi: OneUiCheck.Info,
    val current: Current,
    val profiles: List<Profile>,
    val busy: Boolean,
    val message: String,
    val notifGranted: Boolean,
    val a11yEnabled: Boolean,
    val wallpapers: List<WallpaperApp>,
    val overlays: List<Overlay>
)

class Actions(
    val tweak: (String, () -> ShizukuShell.Result) -> Unit,
    val connect: () -> Unit,
    val openShizuku: () -> Unit,
    val requestNotifications: () -> Unit,
    val openAccessibility: () -> Unit,
    val refresh: () -> Unit,
    val setLauncher: (String) -> Unit,
    val saveProfile: (Profile) -> Unit,
    val deleteProfile: (String) -> Unit,
    val setLiveWallpaper: (WallpaperApp) -> Unit,
    val openWallpaperPicker: () -> Unit,
    val openFontSettings: () -> Unit,
    val loadOverlays: () -> Unit,
    val toggleOverlay: (String, Boolean) -> Unit
)

enum class Tab(val label: String, val subtitle: String, val icon: ImageVector) {
    HOME("Home", "Setup and quick controls", Icons.Rounded.Home),
    DISPLAY("Display", "Text, density, brightness and screen", Icons.Rounded.Tune),
    MOTION("Motion", "Animations and refresh rate", Icons.Rounded.Speed),
    TOOLS("Tools", "Wallpaper, battery, apps and more", Icons.Rounded.Build),
    PROFILES("Profiles", "Save and apply full setups", Icons.Rounded.Star)
}


// ---------- Root ----------

@Composable
fun AppRoot(state: UiState, actions: Actions) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    val tab = Tab.entries[tabIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = i == tabIndex,
                        onClick = { tabIndex = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (tab == Tab.HOME) Hero(state) else PageHeader(tab)
            if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            MessageBar(state.message)

            when (tab) {
                Tab.HOME -> HomeTab(state, actions)
                Tab.DISPLAY -> DisplayTab(state, actions)
                Tab.MOTION -> MotionTab(state, actions)
                Tab.TOOLS -> ToolsTab(state, actions)
                Tab.PROFILES -> ProfilesTab(state, actions)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Hero(state: UiState) {
    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Image(
            painter = painterResource(R.drawable.banner_onetweak),
            contentDescription = "OneUI Tweak banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60B1E3F))))
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "OneUI Tweak",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        state.oneUi.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            StatusPill(connected = state.connected)
        }
    }
}

@Composable
private fun StatusPill(connected: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.18f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (connected) GoodGreen else Color(0xFFFFB4AB))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (connected) "Shizuku connected" else "Shizuku not connected",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PageHeader(tab: Tab) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp)) {
        Text(tab.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(tab.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageBar(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ---------- Shared pieces ----------

@Composable
private fun TweakCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(icon)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun IconChip(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    subtitle: String,
    done: Boolean,
    buttonLabel: String?,
    onAction: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (done) GoodGreen else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (buttonLabel != null) {
            FilledTonalButton(onClick = onAction) { Text(buttonLabel) }
        }
    }
}

// ---------- Home ----------

@Composable
private fun HomeTab(state: UiState, actions: Actions) {
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var bluetooth by remember { mutableStateOf(true) }
    var mobileData by remember { mutableStateOf(true) }

    TweakCard("Setup", Icons.Rounded.CheckCircle, "Finish these once to unlock every tweak.") {
        ChecklistRow(
            "Shizuku",
            "Gives OneUI Tweak system-level control without root",
            state.connected,
            if (state.connected) null else "Connect"
        ) { actions.connect() }
        ChecklistRow(
            "Notifications", "Shows a message when Shizuku connects",
            state.notifGranted, if (state.notifGranted) null else "Allow"
        ) { actions.requestNotifications() }
        ChecklistRow(
            "Accessibility", "Turn on OneUI Tweak in accessibility settings",
            state.a11yEnabled, if (state.a11yEnabled) null else "Enable"
        ) { actions.openAccessibility() }
        if (!state.connected) {
            OutlinedButton(onClick = actions.openShizuku, modifier = Modifier.fillMaxWidth()) {
                Text("Open Shizuku app")
            }
        }
        if (!state.oneUi.supported) {
            Text(
                "This app supports One UI 8 and One UI 9 only.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    TweakCard("Quick toggles", Icons.Rounded.Bolt, "Turn these on or off instantly.") {
        SwitchRow(Icons.Rounded.Bluetooth, "Bluetooth", bluetooth, canUse) { on ->
            bluetooth = on
            actions.tweak("Bluetooth") { Tweaks.setBluetooth(on) }
        }
        SwitchRow(Icons.Rounded.SignalCellularAlt, "Mobile data", mobileData, canUse) { on ->
            mobileData = on
            actions.tweak("Mobile data") { Tweaks.setMobileData(on) }
        }
    }

    TweakCard("Reset", Icons.Rounded.Refresh, "Restores motion, text, density, dark mode, refresh rate, rotation, timeout, touch overlays and battery saver.") {
        Button(
            enabled = canUse,
            onClick = { actions.tweak("Reset") { Tweaks.resetAll() } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset everything") }
    }

    TweakCard("About", Icons.Rounded.Info) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
            )
            Column {
                Text("OneUI Tweak 4.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Shizuku-powered. No root.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------- Display ----------

@Composable
private fun DisplayTab(state: UiState, actions: Actions) {
    val current = state.current
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var font by remember(current.font) { mutableFloatStateOf(current.font) }
    var dpi by remember { mutableStateOf("") }
    var brightness by remember { mutableFloatStateOf(128f) }
    var hiddenIcons by remember { mutableStateOf(setOf<String>()) }

    TweakCard("Text size", Icons.Rounded.TextFields, "Changes the system font scale. Swapping the typeface itself needs root, so it isn't offered.") {
        Text("Scale: ${"%.2f".format(font)}x", style = MaterialTheme.typography.bodyLarge)
        Slider(value = font, onValueChange = { font = it }, valueRange = 0.85f..1.3f, steps = 8, enabled = canUse)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Text size") { Tweaks.setFontScale(font) } }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = {
                font = 1f
                actions.tweak("Text size") { Tweaks.setFontScale(1f) }
            }) { Text("Normal") }
        }
    }

    TweakCard("Screen density", Icons.Rounded.AspectRatio, "Range 120 to 800.") {
        OutlinedTextField(
            value = dpi,
            onValueChange = { dpi = it.filter(Char::isDigit) },
            label = { Text("DPI") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Density") { Tweaks.setDensity(dpi.toIntOrNull()) } }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Density") { Tweaks.resetDensity() } }) { Text("Reset") }
        }
    }

    TweakCard("Status bar icons", Icons.Rounded.Notifications, "Hides chosen icons from the status bar. Samsung may ignore some of these.") {
        listOf(
            "wifi" to "Wi-Fi", "bluetooth" to "Bluetooth", "alarm_clock" to "Alarm",
            "location" to "Location", "hotspot" to "Hotspot", "vpn" to "VPN", "nfc" to "NFC"
        ).forEach { (key, label) ->
            SwitchRow(Icons.Rounded.Notifications, label, key !in hiddenIcons, canUse) { shown ->
                hiddenIcons = if (shown) hiddenIcons - key else hiddenIcons + key
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = {
                val list = hiddenIcons.toList()
                actions.tweak("Status icons") { Tweaks.setHiddenStatusIcons(list) }
            }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = {
                hiddenIcons = emptySet()
                actions.tweak("Status icons") { Tweaks.setHiddenStatusIcons(emptyList()) }
            }) { Text("Show all") }
        }
    }

    TweakCard("Fonts", Icons.Rounded.TextFields, "Galaxy Themes installs custom fonts on One UI. This opens it for you.") {
        Button(onClick = actions.openFontSettings, modifier = Modifier.fillMaxWidth()) { Text("Open font settings") }
    }

    TweakCard("Dark mode", Icons.Rounded.DarkMode) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Dark mode") { Tweaks.setNightMode("yes") } }) { Text("On") }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Dark mode") { Tweaks.setNightMode("no") } }) { Text("Off") }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Dark mode") { Tweaks.setNightMode("auto") } }) { Text("Auto") }
        }
    }

    TweakCard("Brightness", Icons.Rounded.Brightness6, "Turns off auto-brightness and sets a fixed level.") {
        Text("Level: ${brightness.toInt()} / 255", style = MaterialTheme.typography.bodyLarge)
        Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0f..255f, enabled = canUse)
        Button(enabled = canUse, onClick = {
            val level = brightness.toInt()
            actions.tweak("Brightness") { Tweaks.setBrightness(level) }
        }) { Text("Apply") }
    }

    TweakCard("Screen behavior", Icons.Rounded.ScreenRotation) {
        SwitchRow(Icons.Rounded.ScreenRotation, "Auto-rotate", current.autoRotate, canUse) { on ->
            actions.tweak("Auto-rotate") { Tweaks.setAutoRotate(on) }
        }
        SwitchRow(Icons.Rounded.TouchApp, "Show touches", false, canUse) { on ->
            actions.tweak("Show touches") { Tweaks.setShowTouches(on) }
        }
        SwitchRow(Icons.Rounded.Palette, "Pointer location", false, canUse) { on ->
            actions.tweak("Pointer location") { Tweaks.setPointerLocation(on) }
        }
        SwitchRow(Icons.Rounded.LocalCafe, "Stay awake while charging", false, canUse) { on ->
            actions.tweak("Stay awake") { Tweaks.setStayAwake(on) }
        }
    }

    TweakCard("Screen timeout", Icons.Rounded.Timer, "Current: ${current.timeoutSec}s") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30 to "30s", 60 to "1m", 300 to "5m", 600 to "10m").forEach { (sec, label) ->
                OutlinedButton(enabled = canUse, onClick = {
                    actions.tweak("Screen timeout") { Tweaks.setScreenTimeout(sec) }
                }) { Text(label) }
            }
        }
    }
}

// ---------- Motion ----------

@Composable
private fun MotionTab(state: UiState, actions: Actions) {
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var anim by remember(state.current.anim) { mutableFloatStateOf(state.current.anim) }

    TweakCard("Animation speed", Icons.Rounded.Speed, "Scales window, transition and in-app animations.") {
        Text("Scale: ${"%.2f".format(anim)}x", style = MaterialTheme.typography.bodyLarge)
        Slider(value = anim, onValueChange = { anim = it }, valueRange = 0f..2f, steps = 19, enabled = canUse)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Animation speed") { Tweaks.setAnimationScale(anim) } }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = {
                anim = 1f
                actions.tweak("Animation speed") { Tweaks.setAnimationScale(1f) }
            }) { Text("Normal") }
        }
    }

    TweakCard("Animation presets", Icons.Rounded.Bolt) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Off" to 0f, "Fast" to 0.5f, "Normal" to 1f, "Slow" to 1.5f).forEach { (label, v) ->
                FilledTonalButton(enabled = canUse, onClick = {
                    anim = v
                    actions.tweak("Preset $label") { Tweaks.setAnimationScale(v) }
                }) { Text(label) }
            }
        }
    }

    TweakCard("Refresh rate", Icons.Rounded.Autorenew, "Sets the allowed range. Some phones or apps may ignore a fixed rate.") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(60 to "60 Hz", 90 to "90 Hz", 120 to "120 Hz").forEach { (hz, label) ->
                OutlinedButton(enabled = canUse, onClick = {
                    actions.tweak("Refresh $label") { Tweaks.setRefreshRate(hz, hz) }
                }) { Text(label) }
            }
        }
        Button(enabled = canUse, onClick = {
            actions.tweak("Refresh auto") { Tweaks.setRefreshRate(60, 120) }
        }, modifier = Modifier.fillMaxWidth()) { Text("Auto (60 to 120 Hz)") }
    }
}

// ---------- Tools ----------

@Composable
private fun ToolsTab(state: UiState, actions: Actions) {
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var pkg by remember { mutableStateOf("") }
    var battery by remember { mutableStateOf(false) }
    var wifi by remember { mutableStateOf(true) }

    TweakCard("Live wallpaper", Icons.Rounded.Image, "Pick an installed live wallpaper, then confirm it on the next screen. No Shizuku needed.") {
        if (state.wallpapers.isEmpty()) {
            Text("No live wallpapers found on this phone.", style = MaterialTheme.typography.bodySmall)
        }
        state.wallpapers.take(8).forEach { w ->
            OutlinedButton(onClick = { actions.setLiveWallpaper(w) }, modifier = Modifier.fillMaxWidth()) {
                Text(w.label)
            }
        }
        Button(onClick = actions.openWallpaperPicker, modifier = Modifier.fillMaxWidth()) {
            Text("Open wallpaper picker")
        }
    }

    TweakCard("Battery", Icons.Rounded.BatteryChargingFull) {
        SwitchRow(Icons.Rounded.BatteryChargingFull, "Battery saver", battery, canUse) { on ->
            battery = on
            actions.tweak("Battery saver") { Tweaks.setBatterySaver(on) }
        }
    }

    TweakCard("Wi-Fi", Icons.Rounded.Wifi, "Turning Wi-Fi off also turns off your internet.") {
        SwitchRow(Icons.Rounded.Wifi, "Wi-Fi", wifi, canUse) { on ->
            wifi = on
            actions.tweak("Wi-Fi") { Tweaks.setWifi(on) }
        }
    }

    TweakCard("Enable or disable an app", Icons.Rounded.Apps, "Uses Shizuku for user 0. Disabling system apps can break things.") {
        OutlinedTextField(
            value = pkg,
            onValueChange = { pkg = it.trim() },
            label = { Text("Package name, e.g. com.example.app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Disable $pkg") { Tweaks.setAppEnabled(pkg, false) } }) { Text("Disable") }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Enable $pkg") { Tweaks.setAppEnabled(pkg, true) } }) { Text("Enable") }
        }
    }

    TweakCard("Theme overlays", Icons.Rounded.Palette, "Turn on theme overlays installed from other apps, such as Layers or Galaxy Themes packs. Samsung may ignore some overlays.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = state.connected && !state.busy, onClick = actions.loadOverlays) { Text("Load overlays") }
        }
        if (state.overlays.isEmpty()) {
            Text("No overlays loaded yet. Install a theme overlay, then tap Load overlays.", style = MaterialTheme.typography.bodySmall)
        }
        state.overlays.take(12).forEach { o ->
            SwitchRow(Icons.Rounded.Palette, o.pkg, o.enabled, canUse) { on -> actions.toggleOverlay(o.pkg, on) }
        }
    }

    TweakCard("OneUI Tweak icon", Icons.Rounded.Palette, "Changes only this app's icon. No Shizuku needed.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { actions.setLauncher("LauncherDefault") }) { Text("Blue") }
            OutlinedButton(onClick = { actions.setLauncher("LauncherDark") }) { Text("Navy") }
        }
    }
}

// ---------- Profiles ----------

@Composable
private fun ProfilesTab(state: UiState, actions: Actions) {
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var name by remember { mutableStateOf("") }
    var night by remember { mutableStateOf("auto") }

    TweakCard("Save current setup", Icons.Rounded.Save, "Stores motion, text size, dark mode, rotation and timeout. Connect first so values load.") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("yes", "no", "auto").forEach { m ->
                FilterChip(selected = night == m, onClick = { night = m }, label = { Text(m) })
            }
        }
        Button(
            enabled = canUse && name.isNotBlank(),
            onClick = {
                val c = state.current
                actions.saveProfile(Profile(name.trim(), c.anim, c.font, night, c.autoRotate, c.timeoutSec))
                name = ""
            }
        ) { Text("Save profile") }
    }

    if (state.profiles.isEmpty()) {
        TweakCard("No profiles yet", Icons.Rounded.Star) {
            Text("Save your favorite setup once, then apply it in one tap.")
        }
    }

    state.profiles.forEach { p ->
        TweakCard(
            p.name,
            Icons.Rounded.Star,
            "Motion ${"%.2f".format(p.anim)}x · Text ${"%.2f".format(p.font)}x · Dark ${p.nightMode} · " +
                "Rotate ${if (p.autoRotate) "on" else "off"} · Screen ${p.timeoutSec}s"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = { actions.tweak("Profile ${p.name}") { Tweaks.applyProfile(p) } }) { Text("Apply") }
                TextButton(onClick = { actions.deleteProfile(p.name) }) { Text("Delete") }
            }
        }
    }
}
