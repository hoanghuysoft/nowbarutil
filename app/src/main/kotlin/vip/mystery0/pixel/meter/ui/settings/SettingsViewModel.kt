package com.kakao.taxi.ui.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.kakao.taxi.data.repository.OrderRepository
import android.os.PowerManager as AndroidPowerManager

class SettingsViewModel(
    private val application: Application,
) : AndroidViewModel(application), KoinComponent {
    private val repository: OrderRepository by inject()
    private val powerManager: AndroidPowerManager by inject()

    val hasNotificationPermission = MutableStateFlow(true)
    val isIgnoringBatteryOptimizations = MutableStateFlow(true)
    val canEnableAutoStart = MutableStateFlow(false)

    val isServiceRunning = repository.isMonitoring

    // API
    val apiKey = repository.apiKey
    val pollingInterval = repository.pollingInterval

    // Notification Settings
    val isNotificationEnabled = repository.isNotificationEnabled
    val isLiveUpdateEnabled = repository.isLiveUpdateEnabled
    val notificationTextSize = repository.notificationTextSize
    val notificationUnitSize = repository.notificationUnitSize
    val notificationUseCustomColor = repository.notificationUseCustomColor
    val notificationColor = repository.notificationColor
    val isBlankNotificationEnabled = repository.isBlankNotificationEnabled

    // General Settings
    val isHideFromRecents = repository.isHideFromRecents
    val isAutoStartServiceEnabled = repository.isAutoStartServiceEnabled
    val isOledThemeEnabled = repository.isOledThemeEnabled

    init {
        refreshSettings()
    }

    fun refreshSettings() {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasNotificationPermission.value = ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                hasNotificationPermission.value = true
            }
            isIgnoringBatteryOptimizations.value =
                powerManager.isIgnoringBatteryOptimizations(application.packageName)
            checkAutoStartPermissions()
        }
    }

    private fun checkAutoStartPermissions() {
        val hasNotif = hasNotificationPermission.value
        canEnableAutoStart.value = hasNotif

        if (!hasNotif && repository.isAutoStartServiceEnabled.value) {
            setAutoStartServiceEnabled(false)
        }
    }

    fun setApiKey(key: String) = repository.setApiKey(key)
    fun setPollingInterval(interval: Long) = repository.setPollingInterval(interval)

    fun setHideFromRecents(hide: Boolean) = repository.setHideFromRecents(hide)
    fun setNotificationEnabled(enabled: Boolean) = repository.setNotificationEnabled(enabled)
    fun setLiveUpdateEnabled(enabled: Boolean) = repository.setLiveUpdateEnabled(enabled)
    fun setBlankNotificationEnabled(enabled: Boolean) = repository.setBlankNotificationEnabled(enabled)
    fun setNotificationTextSize(size: Float) = repository.setNotificationTextSize(size)
    fun setNotificationUnitSize(size: Float) = repository.setNotificationUnitSize(size)
    fun setNotificationUseCustomColor(useCustom: Boolean) = repository.setNotificationUseCustomColor(useCustom)
    fun setNotificationColor(color: Int) = repository.setNotificationColor(color)
    fun setAutoStartServiceEnabled(enabled: Boolean) = repository.setAutoStartServiceEnabled(enabled)
    fun setOledThemeEnabled(enabled: Boolean) = repository.setOledThemeEnabled(enabled)
}
