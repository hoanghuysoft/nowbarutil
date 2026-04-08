package com.kakao.taxi.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.kakao.taxi.BuildConfig
import com.kakao.taxi.R
import com.kakao.taxi.ui.theme.PixelPulseTheme

val topShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
val middleShape = RoundedCornerShape(4.dp)
val bottomShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
val singleShape = RoundedCornerShape(20.dp)

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

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
                        FilledTonalIconButton(
                            onClick = { finish() },
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
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

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    shape: Shape,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "press"
    )

    val animatedShape = remember(shape, pressProgress) {
        if (shape is RoundedCornerShape && onClick != null) {
            object : Shape {
                override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                    val targetPx = with(density) { 20.dp.toPx() }
                    fun lerp(start: Float, stop: Float, fraction: Float) =
                        (1 - fraction) * start + fraction * stop

                    val ts = lerp(shape.topStart.toPx(size, density), targetPx, pressProgress)
                    val te = lerp(shape.topEnd.toPx(size, density), targetPx, pressProgress)
                    val bs = lerp(shape.bottomStart.toPx(size, density), targetPx, pressProgress)
                    val be = lerp(shape.bottomEnd.toPx(size, density), targetPx, pressProgress)

                    return Outline.Rounded(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                            topLeft = androidx.compose.ui.geometry.CornerRadius(ts),
                            topRight = androidx.compose.ui.geometry.CornerRadius(te),
                            bottomRight = androidx.compose.ui.geometry.CornerRadius(be),
                            bottomLeft = androidx.compose.ui.geometry.CornerRadius(bs)
                        )
                    )
                }
            }
        } else shape
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(animatedShape),
        shape = animatedShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (content != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.titleMedium)
                        if (subtitle.isNotEmpty()) {
                            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        } else {
            var modifier = Modifier.fillMaxWidth()
            if (onClick != null) {
                modifier = modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
            }
            ListItem(
                headlineContent = {
                    Text(text = title, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    if (subtitle.isNotEmpty()) {
                        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                    }
                },
                trailingContent = trailingContent,
                modifier = modifier.padding(vertical = 4.dp),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

// ── API Configuration ──

@Composable
fun ApiSection(viewModel: SettingsViewModel) {
    val apiKey by viewModel.apiKey.collectAsState(initial = "")
    val pollingInterval by viewModel.pollingInterval.collectAsState(initial = 30000L)
    var showApiKeyDialog by remember { mutableStateOf(false) }

    SectionTitle(stringResource(R.string.settings_category_api))

    SettingsItemCard(
        icon = Icons.Default.Key,
        title = stringResource(R.string.settings_api_key_title),
        subtitle = if (apiKey.isBlank()) stringResource(R.string.settings_api_key_not_configured) else "••••••••${apiKey.takeLast(6)}",
        containerColor = Color(0xFFE3F2FD),
        iconColor = Color(0xFF1565C0),
        shape = topShape,
        onClick = { showApiKeyDialog = true }
    )
    Spacer(modifier = Modifier.height(2.dp))

    val pollingSeconds = (pollingInterval / 1000f).coerceIn(10f, 120f)
    SettingsItemCard(
        icon = Icons.Default.Timer,
        title = stringResource(R.string.settings_polling_interval_title),
        subtitle = stringResource(R.string.settings_polling_interval_desc) + " (${pollingSeconds.toInt()}s)",
        containerColor = Color(0xFFE1F5FE),
        iconColor = Color(0xFF0277BD),
        shape = bottomShape
    ) {
        Slider(
            value = pollingSeconds,
            onValueChange = { viewModel.setPollingInterval((it * 1000).toLong()) },
            valueRange = 30f..120f,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(stringResource(R.string.settings_api_key_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.settings_api_key_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(stringResource(R.string.settings_api_key_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setApiKey(keyInput.trim())
                    showApiKeyDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text(stringResource(R.string.action_cancel)) }
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

    SectionTitle(stringResource(R.string.settings_category_general))

    SettingsItemCard(
        icon = Icons.Default.DarkMode,
        title = stringResource(R.string.settings_oled_theme_title),
        subtitle = stringResource(R.string.settings_oled_theme_desc),
        containerColor = Color(0xFFF3E5F5),
        iconColor = Color(0xFF6A1B9A),
        shape = topShape,
        trailingContent = {
            Switch(checked = isOledThemeEnabled, onCheckedChange = { viewModel.setOledThemeEnabled(it) })
        }
    )
    Spacer(modifier = Modifier.height(2.dp))

    val autoStartSummary = if (canEnableAutoStart) stringResource(R.string.settings_auto_start_service_desc) else stringResource(R.string.settings_auto_start_disabled_reason)
    SettingsItemCard(
        icon = Icons.Default.Autorenew,
        title = stringResource(R.string.settings_auto_start_service_title),
        subtitle = autoStartSummary,
        containerColor = Color(0xFFEDE7F6),
        iconColor = Color(0xFF4527A0),
        shape = middleShape,
        trailingContent = {
            Switch(checked = isAutoStartEnabled, onCheckedChange = { viewModel.setAutoStartServiceEnabled(it) }, enabled = canEnableAutoStart)
        }
    )
    Spacer(modifier = Modifier.height(2.dp))

    val notificationPermissionSummary = if (hasNotificationPermission) stringResource(R.string.settings_permission_granted) else stringResource(R.string.settings_permission_denied)
    SettingsItemCard(
        icon = Icons.Default.Notifications,
        title = stringResource(R.string.settings_permission_notification),
        subtitle = notificationPermissionSummary,
        containerColor = Color(0xFFE8EAF6),
        iconColor = Color(0xFF283593),
        shape = bottomShape,
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
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsState(initial = true)

    SectionTitle(stringResource(R.string.settings_category_background))

    SettingsItemCard(
        icon = Icons.Default.VisibilityOff,
        title = stringResource(R.string.settings_hide_from_recents_title),
        subtitle = stringResource(R.string.settings_hide_from_recents_desc),
        containerColor = Color(0xFFE8F5E9),
        iconColor = Color(0xFF2E7D32),
        shape = topShape,
        trailingContent = {
            Switch(checked = isHideFromRecents, onCheckedChange = { viewModel.setHideFromRecents(it) })
        }
    )
    Spacer(modifier = Modifier.height(2.dp))

    val batterySummary = if (isIgnoringBatteryOptimizations) stringResource(R.string.settings_battery_optimization_disabled) else stringResource(R.string.settings_battery_optimization_enabled)
    SettingsItemCard(
        icon = Icons.Default.BatterySaver,
        title = stringResource(R.string.settings_battery_optimization_title),
        subtitle = batterySummary,
        containerColor = Color(0xFFF1F8E9),
        iconColor = Color(0xFF558B2F),
        shape = bottomShape,
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
    val useCustomColor by viewModel.notificationUseCustomColor.collectAsState(initial = false)
    val notificationColor by viewModel.notificationColor.collectAsState(initial = 0)

    SectionTitle(stringResource(R.string.settings_category_notification))

    SettingsItemCard(
        icon = Icons.Default.NotificationsActive,
        title = stringResource(R.string.config_enable_notification),
        subtitle = stringResource(R.string.config_enable_notification_desc),
        containerColor = Color(0xFFFFEBEE),
        iconColor = Color(0xFFC62828),
        shape = if (isEnabled) topShape else singleShape,
        trailingContent = {
            Switch(checked = isEnabled, onCheckedChange = { viewModel.setNotificationEnabled(it) })
        }
    )

    if (isEnabled) {
        Spacer(modifier = Modifier.height(2.dp))
        SettingsItemCard(
            icon = Icons.Default.IntegrationInstructions,
            title = stringResource(R.string.config_enable_live_update),
            subtitle = stringResource(R.string.config_enable_live_update_desc),
            containerColor = Color(0xFFFCE4EC),
            iconColor = Color(0xFFAD1457),
            shape = middleShape,
            trailingContent = {
                Switch(checked = isLiveUpdateEnabled, onCheckedChange = { viewModel.setLiveUpdateEnabled(it) }, enabled = !isBlankNotificationEnabled)
            }
        )
        Spacer(modifier = Modifier.height(2.dp))

        SettingsItemCard(
            icon = Icons.Default.VisibilityOff,
            title = stringResource(R.string.settings_blank_notification_title),
            subtitle = stringResource(R.string.settings_blank_notification_desc),
            containerColor = Color(0xFFF3E5F5),
            iconColor = Color(0xFF6A1B9A),
            shape = middleShape,
            trailingContent = {
                Switch(checked = isBlankNotificationEnabled, onCheckedChange = { viewModel.setBlankNotificationEnabled(it) }, enabled = !isLiveUpdateEnabled)
            }
        )
        Spacer(modifier = Modifier.height(2.dp))

        SettingsItemCard(
            icon = Icons.Default.FormatSize,
            title = stringResource(R.string.settings_notification_text_size),
            subtitle = "%.2f".format(textSize),
            containerColor = Color(0xFFE8EAF6),
            iconColor = Color(0xFF283593),
            shape = middleShape
        ) {
            Slider(
                value = textSize,
                onValueChange = { viewModel.setNotificationTextSize(it) },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLiveUpdateEnabled
            )
        }
        Spacer(modifier = Modifier.height(2.dp))

        SettingsItemCard(
            icon = Icons.Default.FormatSize,
            title = stringResource(R.string.settings_notification_unit_size),
            subtitle = "%.2f".format(unitSize),
            containerColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF1565C0),
            shape = middleShape
        ) {
            Slider(
                value = unitSize,
                onValueChange = { viewModel.setNotificationUnitSize(it) },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLiveUpdateEnabled
            )
        }
        Spacer(modifier = Modifier.height(2.dp))

        SettingsItemCard(
            icon = Icons.Default.ColorLens,
            title = stringResource(R.string.settings_notification_use_custom_color_title),
            subtitle = stringResource(R.string.settings_notification_use_custom_color_desc),
            containerColor = Color(0xFFE1F5FE),
            iconColor = Color(0xFF0277BD),
            shape = middleShape,
            trailingContent = {
                Switch(checked = useCustomColor, onCheckedChange = { viewModel.setNotificationUseCustomColor(it) })
            }
        )
        Spacer(modifier = Modifier.height(2.dp))

        var showColorDialog by remember { mutableStateOf(false) }
        SettingsItemCard(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.settings_notification_color_title),
            subtitle = "Tap to pick color",
            containerColor = Color(0xFFE0F7FA),
            iconColor = Color(0xFF00695C),
            shape = bottomShape,
            onClick = { if (useCustomColor) showColorDialog = true },
            trailingContent = {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(notificationColor))
                )
            }
        )

        if (showColorDialog) {
            val controller = rememberColorPickerController()
            var selectedColor by remember { mutableStateOf(Color(notificationColor)) }
            AlertDialog(
                onDismissRequest = { showColorDialog = false },
                title = { Text(stringResource(R.string.color_picker_title)) },
                text = {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HsvColorPicker(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            controller = controller,
                            initialColor = Color(notificationColor),
                            onColorChanged = { envelope -> selectedColor = envelope.color }
                        )
                        AlphaSlider(modifier = Modifier.fillMaxWidth().height(36.dp), controller = controller)
                        BrightnessSlider(modifier = Modifier.fillMaxWidth().height(36.dp), controller = controller)
                        AlphaTile(modifier = Modifier.fillMaxWidth().height(36.dp).clip(MaterialTheme.shapes.medium), controller = controller)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setNotificationColor(selectedColor.toArgb())
                        showColorDialog = false
                    }) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showColorDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                }
            )
        }
    }
}

// ── About ──

@Composable
fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    SectionTitle(stringResource(R.string.settings_category_about))
    
    SettingsItemCard(
        icon = Icons.Default.Info,
        title = stringResource(R.string.settings_app_version),
        subtitle = BuildConfig.VERSION_NAME,
        containerColor = Color(0xFFFFF3E0),
        iconColor = Color(0xFFE65100),
        shape = topShape
    )
    Spacer(modifier = Modifier.height(2.dp))
    
    SettingsItemCard(
        icon = Icons.Default.Coffee,
        title = stringResource(R.string.settings_github),
        subtitle = "https://ko-fi.com/koshiellen",
        containerColor = Color(0xFFFBE9E7),
        iconColor = Color(0xFFD84315),
        shape = bottomShape,
        onClick = { uriHandler.openUri("https://ko-fi.com/koshiellen") }
    )
}
