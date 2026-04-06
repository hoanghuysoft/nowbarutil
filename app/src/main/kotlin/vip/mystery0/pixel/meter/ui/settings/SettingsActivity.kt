package com.kakao.taxi.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TwoTargetPreference
import com.kakao.taxi.BuildConfig
import com.kakao.taxi.R
import com.kakao.taxi.ui.theme.PixelPulseTheme
import java.util.Locale

class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isOledTheme by viewModel.isOledThemeEnabled.collectAsState(initial = false)
            PixelPulseTheme(isOledTheme = isOledTheme) {
                SettingsScreen()
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshSettings()
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_settings)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            ProvidePreferenceLocals {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item { ApiSection(viewModel) }
                    item { GeneralSection(viewModel) }
                    item { NotificationSection(viewModel) }
                    item { BackgroundSection(viewModel) }
                    item { AboutSection() }
                    item {
                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }
}

// ── API Configuration ──

@Composable
fun ApiSection(viewModel: SettingsViewModel) {
    val apiKey by viewModel.apiKey.collectAsState(initial = "")
    val pollingInterval by viewModel.pollingInterval.collectAsState(initial = 30000L)
    var showApiKeyDialog by remember { mutableStateOf(false) }

    PreferenceCategory(title = { Text("API Configuration") })

    Preference(
        title = { Text("API Key") },
        summary = {
            Text(
                if (apiKey.isBlank()) "Not configured — tap to set"
                else "••••••••${apiKey.takeLast(6)}"
            )
        },
        onClick = { showApiKeyDialog = true }
    )

    // Polling interval slider (10s to 120s)
    val pollingSeconds = (pollingInterval / 1000f).coerceIn(10f, 120f)
    SliderPreference(
        value = 0F,
        onValueChange = { },
        sliderValue = pollingSeconds,
        onSliderValueChange = { viewModel.setPollingInterval((it * 1000).toLong()) },
        valueRange = 10f..120f,
        title = { Text("Polling Interval") },
        summary = { Text("How often to check order status") },
        valueText = { Text("${pollingSeconds.toInt()}s") }
    )

    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("API Key") },
            text = {
                Column {
                    Text(
                        "Enter your express.io.vn API key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("X-API-Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setApiKey(keyInput.trim())
                    showApiKeyDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── General Settings ──

@Composable
fun GeneralSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val isAutoStartEnabled by viewModel.isAutoStartServiceEnabled.collectAsState(initial = false)
    val isOledThemeEnabled by viewModel.isOledThemeEnabled.collectAsState(initial = false)
    val canEnableAutoStart by viewModel.canEnableAutoStart.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()

    PreferenceCategory(title = { Text(stringResource(R.string.settings_category_general)) })

    SwitchPreference(
        value = isOledThemeEnabled,
        onValueChange = { viewModel.setOledThemeEnabled(it) },
        title = { Text(stringResource(R.string.settings_oled_theme_title)) },
        summary = { Text(stringResource(R.string.settings_oled_theme_desc)) }
    )

    val autoStartSummary = if (canEnableAutoStart) {
        stringResource(R.string.settings_auto_start_service_desc)
    } else {
        stringResource(R.string.settings_auto_start_disabled_reason)
    }

    SwitchPreference(
        value = isAutoStartEnabled,
        onValueChange = { viewModel.setAutoStartServiceEnabled(it) },
        enabled = canEnableAutoStart,
        title = { Text(stringResource(R.string.settings_auto_start_service_title)) },
        summary = { Text(autoStartSummary) }
    )

    val notificationPermissionSummary = if (hasNotificationPermission) {
        stringResource(R.string.settings_permission_granted)
    } else {
        stringResource(R.string.settings_permission_denied)
    }
    Preference(
        title = { Text(stringResource(R.string.settings_permission_notification)) },
        summary = { Text(notificationPermissionSummary) },
        onClick = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        }
    )
}

// ── Background Settings ──

@Composable
fun BackgroundSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val isHideFromRecents by viewModel.isHideFromRecents.collectAsState(initial = false)
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsState(
        initial = true
    )

    PreferenceCategory(title = { Text(stringResource(R.string.settings_category_background)) })

    SwitchPreference(
        value = isHideFromRecents,
        onValueChange = { viewModel.setHideFromRecents(it) },
        title = { Text(stringResource(R.string.settings_hide_from_recents_title)) },
        summary = { Text(stringResource(R.string.settings_hide_from_recents_desc)) }
    )

    val batterySummary = if (isIgnoringBatteryOptimizations) {
        stringResource(R.string.settings_battery_optimization_disabled)
    } else {
        stringResource(R.string.settings_battery_optimization_enabled)
    }
    Preference(
        title = { Text(stringResource(R.string.settings_battery_optimization_title)) },
        summary = { Text(batterySummary) },
        onClick = {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        }
    )
}

// ── Notification Settings ──

@Composable
fun NotificationSection(viewModel: SettingsViewModel) {
    val isEnabled by viewModel.isNotificationEnabled.collectAsState(initial = true)
    val isLiveUpdateEnabled by viewModel.isLiveUpdateEnabled.collectAsState(initial = true)
    val textSize by viewModel.notificationTextSize.collectAsState(initial = 0.60f)
    val unitSize by viewModel.notificationUnitSize.collectAsState(initial = 0.45f)
    val isBlankNotificationEnabled by viewModel.isBlankNotificationEnabled.collectAsState(initial = false)

    PreferenceCategory(title = { Text(stringResource(R.string.settings_category_notification)) })
    SwitchPreference(
        value = isEnabled,
        onValueChange = { viewModel.setNotificationEnabled(it) },
        title = { Text(stringResource(R.string.config_enable_notification)) },
        summary = { Text(stringResource(R.string.config_enable_notification_desc)) }
    )

    if (isEnabled) {
        SwitchPreference(
            value = isLiveUpdateEnabled,
            onValueChange = { viewModel.setLiveUpdateEnabled(it) },
            enabled = !isBlankNotificationEnabled,
            title = { Text(stringResource(R.string.config_enable_live_update)) },
            summary = { Text(stringResource(R.string.config_enable_live_update_desc)) }
        )
        SwitchPreference(
            value = isBlankNotificationEnabled,
            onValueChange = { viewModel.setBlankNotificationEnabled(it) },
            enabled = !isLiveUpdateEnabled,
            title = { Text(stringResource(R.string.settings_blank_notification_title)) },
            summary = { Text(stringResource(R.string.settings_blank_notification_desc)) }
        )

        SliderPreference(
            enabled = !isLiveUpdateEnabled,
            value = 0F,
            onValueChange = { },
            sliderValue = textSize,
            onSliderValueChange = { viewModel.setNotificationTextSize(it) },
            valueRange = 0.1f..1.0f,
            title = { Text(stringResource(R.string.settings_notification_text_size)) },
            valueText = { Text("%.2f".format(textSize)) }
        )

        SliderPreference(
            enabled = !isLiveUpdateEnabled,
            value = 0F,
            onValueChange = { },
            sliderValue = unitSize,
            onSliderValueChange = { viewModel.setNotificationUnitSize(it) },
            valueRange = 0.1f..1.0f,
            title = { Text(stringResource(R.string.settings_notification_unit_size)) },
            valueText = { Text("%.2f".format(unitSize)) }
        )

        // Notification Color Settings
        val useCustomColor by viewModel.notificationUseCustomColor.collectAsState(initial = false)
        val notificationColor by viewModel.notificationColor.collectAsState(initial = 0)

        SwitchPreference(
            value = useCustomColor,
            onValueChange = { viewModel.setNotificationUseCustomColor(it) },
            title = { Text(stringResource(R.string.settings_notification_use_custom_color_title)) },
            summary = { Text(stringResource(R.string.settings_notification_use_custom_color_desc)) }
        )

        ColorPreference(
            title = stringResource(R.string.settings_notification_color_title),
            color = Color(notificationColor),
            enabled = useCustomColor,
            onColorSelected = { viewModel.setNotificationColor(it.toArgb()) }
        )
    }
}

// ── About ──

@Composable
fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    PreferenceCategory(title = { Text(stringResource(R.string.settings_category_about)) })
    Preference(
        title = { Text(stringResource(R.string.settings_app_version)) },
        summary = { Text(BuildConfig.VERSION_NAME) }
    )
    Preference(
        title = { Text(stringResource(R.string.settings_github)) },
        summary = { Text("https://github.com/realMoai/NowbarMeter") },
        onClick = { uriHandler.openUri("https://github.com/realMoai/NowbarMeter") }
    )
}

// ── Color Picker ──

@Composable
fun ColorPreference(
    title: String,
    color: Color,
    enabled: Boolean = true,
    onColorSelected: (Color) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val theme = LocalPreferenceTheme.current

    TwoTargetPreference(
        title = { Text(title) },
        enabled = enabled,
        secondTarget = {
            Box(
                modifier = Modifier
                    .padding(horizontal = theme.horizontalSpacing)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        },
        onClick = { if (enabled) showDialog = true }
    )
    if (showDialog) {
        val controller = rememberColorPickerController()
        var selectedColor by remember { mutableStateOf(color) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.color_picker_title)) },
            text = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        controller = controller,
                        initialColor = color,
                        onColorChanged = { envelope ->
                            selectedColor = envelope.color
                        }
                    )
                    AlphaSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        controller = controller,
                    )
                    BrightnessSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        controller = controller,
                    )
                    AlphaTile(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(MaterialTheme.shapes.medium),
                        controller = controller
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onColorSelected(selectedColor)
                    showDialog = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
