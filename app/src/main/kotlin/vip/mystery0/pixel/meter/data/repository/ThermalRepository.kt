package com.kakao.taxi.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import com.kakao.taxi.data.source.ThermalData
import com.kakao.taxi.data.source.impl.BatteryThermalDataSource

class ThermalRepository(
    private val dataSource: BatteryThermalDataSource,
    private val dataStoreRepository: DataStoreRepository,
) : KoinComponent {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitoringJob: Job? = null

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _thermalData = MutableStateFlow(ThermalData(0f))
    val thermalData: StateFlow<ThermalData> = _thermalData.asStateFlow()

    // Existing generic preferences
    private val _isOverlayEnabled = MutableStateFlow(false)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(true)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _isOverlayLocked = MutableStateFlow(false)
    val isOverlayLocked: StateFlow<Boolean> = _isOverlayLocked.asStateFlow()

    private val _overlayBgColor = MutableStateFlow(0xCC000000.toInt())
    val overlayBgColor: StateFlow<Int> = _overlayBgColor.asStateFlow()

    private val _overlayTextColor = MutableStateFlow(0xFFFFFFFF.toInt())
    val overlayTextColor: StateFlow<Int> = _overlayTextColor.asStateFlow()

    private val _overlayCornerRadius = MutableStateFlow(8)
    val overlayCornerRadius: StateFlow<Int> = _overlayCornerRadius.asStateFlow()

    private val _overlayTextSize = MutableStateFlow(10f)
    val overlayTextSize: StateFlow<Float> = _overlayTextSize.asStateFlow()

    private val _notificationTextSize = MutableStateFlow(0.60f)
    val notificationTextSize: StateFlow<Float> = _notificationTextSize.asStateFlow()
    
    private val _notificationUnitSize = MutableStateFlow(0.45f)
    val notificationUnitSize: StateFlow<Float> = _notificationUnitSize.asStateFlow()

    private val _isHideFromRecents = MutableStateFlow(false)
    val isHideFromRecents: StateFlow<Boolean> = _isHideFromRecents.asStateFlow()

    private val _isOverlayUseDefaultColors = MutableStateFlow(false)
    val isOverlayUseDefaultColors: StateFlow<Boolean> = _isOverlayUseDefaultColors.asStateFlow()

    private val _isAutoStartServiceEnabled = MutableStateFlow(false)
    val isAutoStartServiceEnabled: StateFlow<Boolean> = _isAutoStartServiceEnabled.asStateFlow()

    private val _notificationUseCustomColor = MutableStateFlow(false)
    val notificationUseCustomColor: StateFlow<Boolean> = _notificationUseCustomColor.asStateFlow()

    private val _notificationColor = MutableStateFlow(0xFF888888.toInt())
    val notificationColor: StateFlow<Int> = _notificationColor.asStateFlow()

    private val _isLiveUpdateEnabled = MutableStateFlow(true)
    val isLiveUpdateEnabled: StateFlow<Boolean> = _isLiveUpdateEnabled.asStateFlow()

    private val _isBlankNotificationEnabled = MutableStateFlow(false)
    val isBlankNotificationEnabled: StateFlow<Boolean> = _isBlankNotificationEnabled.asStateFlow()

    private val _isOledThemeEnabled = MutableStateFlow(false)
    val isOledThemeEnabled: StateFlow<Boolean> = _isOledThemeEnabled.asStateFlow()

    init {
        runBlocking {
            dataStoreRepository.allPreferences.first().let { prefs ->
                _isNotificationEnabled.value = prefs[DataStoreRepository.KEY_NOTIFICATION_ENABLED] ?: true
                _isOverlayLocked.value = prefs[DataStoreRepository.KEY_OVERLAY_LOCKED] ?: false
                _isOverlayEnabled.value = prefs[DataStoreRepository.KEY_OVERLAY_ENABLED] ?: false
                _overlayBgColor.value = prefs[DataStoreRepository.KEY_OVERLAY_BG_COLOR] ?: 0xCC000000.toInt()
                _overlayTextColor.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_COLOR] ?: 0xFFFFFFFF.toInt()
                _overlayCornerRadius.value = prefs[DataStoreRepository.KEY_OVERLAY_CORNER_RADIUS] ?: 8
                _overlayTextSize.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_SIZE] ?: 10f
                _notificationTextSize.value = prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_SIZE] ?: 0.60f
                _notificationUnitSize.value = prefs[DataStoreRepository.KEY_NOTIFICATION_UNIT_SIZE] ?: 0.45f
                _isHideFromRecents.value = prefs[DataStoreRepository.KEY_HIDE_FROM_RECENTS] ?: false
                _isOverlayUseDefaultColors.value = prefs[DataStoreRepository.KEY_OVERLAY_USE_DEFAULT_COLORS] ?: false
                _isAutoStartServiceEnabled.value = prefs[DataStoreRepository.KEY_AUTO_START_SERVICE] ?: false
                _notificationUseCustomColor.value = prefs[DataStoreRepository.KEY_NOTIFICATION_USE_CUSTOM_COLOR] ?: false
                _notificationColor.value = prefs[DataStoreRepository.KEY_NOTIFICATION_COLOR] ?: 0xFF888888.toInt()
                _isLiveUpdateEnabled.value = prefs[DataStoreRepository.KEY_LIVE_UPDATE] ?: true
                _isBlankNotificationEnabled.value = prefs[DataStoreRepository.KEY_BLANK_NOTIFICATION] ?: false
                _isOledThemeEnabled.value = prefs[DataStoreRepository.KEY_OLED_THEME] ?: false
            }
        }
        
        scope.launch { dataStoreRepository.isNotificationEnabled.collect { _isNotificationEnabled.value = it } }
        scope.launch { dataStoreRepository.isOverlayLocked.collect { _isOverlayLocked.value = it } }
        scope.launch { dataStoreRepository.isOverlayEnabled.collect { _isOverlayEnabled.value = it } }
        scope.launch { dataStoreRepository.overlayBgColor.collect { _overlayBgColor.value = it } }
        scope.launch { dataStoreRepository.overlayTextColor.collect { _overlayTextColor.value = it } }
        scope.launch { dataStoreRepository.overlayCornerRadius.collect { _overlayCornerRadius.value = it } }
        scope.launch { dataStoreRepository.overlayTextSize.collect { _overlayTextSize.value = it } }
        scope.launch { dataStoreRepository.notificationTextSize.collect { _notificationTextSize.value = it } }
        scope.launch { dataStoreRepository.notificationUnitSize.collect { _notificationUnitSize.value = it } }
        scope.launch { dataStoreRepository.isHideFromRecents.collect { _isHideFromRecents.value = it } }
        scope.launch { dataStoreRepository.isOverlayUseDefaultColors.collect { _isOverlayUseDefaultColors.value = it } }
        scope.launch { dataStoreRepository.isAutoStartServiceEnabled.collect { _isAutoStartServiceEnabled.value = it } }
        scope.launch { dataStoreRepository.notificationUseCustomColor.collect { _notificationUseCustomColor.value = it } }
        scope.launch { dataStoreRepository.notificationColor.collect { _notificationColor.value = it } }
        scope.launch { dataStoreRepository.isLiveUpdateEnabled.collect { _isLiveUpdateEnabled.value = it } }
        scope.launch { dataStoreRepository.isBlankNotificationEnabled.collect { _isBlankNotificationEnabled.value = it } }
        scope.launch { dataStoreRepository.isOledThemeEnabled.collect { _isOledThemeEnabled.value = it } }
    }

    fun setOverlayEnabled(enable: Boolean) { scope.launch { dataStoreRepository.setOverlayEnabled(enable) } }
    fun setNotificationEnabled(enable: Boolean) { scope.launch { dataStoreRepository.setNotificationEnabled(enable) } }
    fun setOverlayLocked(locked: Boolean) { scope.launch { dataStoreRepository.setOverlayLocked(locked) } }
    fun setOverlayBgColor(color: Int) { scope.launch { dataStoreRepository.setOverlayBgColor(color) } }
    fun setOverlayTextColor(color: Int) { scope.launch { dataStoreRepository.setOverlayTextColor(color) } }
    fun setOverlayCornerRadius(radius: Int) { scope.launch { dataStoreRepository.setOverlayCornerRadius(radius) } }
    fun setOverlayTextSize(size: Float) { scope.launch { dataStoreRepository.setOverlayTextSize(size) } }
    fun setNotificationTextSize(size: Float) { scope.launch { dataStoreRepository.setNotificationTextSize(size) } }
    fun setNotificationUnitSize(size: Float) { scope.launch { dataStoreRepository.setNotificationUnitSize(size) } }
    fun setHideFromRecents(hide: Boolean) { scope.launch { dataStoreRepository.setHideFromRecents(hide) } }
    fun setOverlayUseDefaultColors(useDefault: Boolean) { scope.launch { dataStoreRepository.setOverlayUseDefaultColors(useDefault) } }
    fun setAutoStartServiceEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setAutoStartServiceEnabled(enabled) } }
    fun setNotificationUseCustomColor(useCustom: Boolean) { scope.launch { dataStoreRepository.setNotificationUseCustomColor(useCustom) } }
    fun setNotificationColor(color: Int) { scope.launch { dataStoreRepository.setNotificationColor(color) } }
    fun setLiveUpdateEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setLiveUpdateEnabled(enabled) } }
    fun setBlankNotificationEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setBlankNotificationEnabled(enabled) } }
    fun setOledThemeEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setOledThemeEnabled(enabled) } }

    suspend fun getOverlayPosition(): Pair<Int, Int> {
        val x = dataStoreRepository.overlayX.first()
        val y = dataStoreRepository.overlayY.first()
        return x to y
    }

    fun saveOverlayPosition(x: Int, y: Int) {
        scope.launch { dataStoreRepository.saveOverlayPosition(x, y) }
    }

    fun startMonitoring() {
        Log.i(TAG, "Request start monitoring")
        if (_isMonitoring.value) return

        _isMonitoring.value = true
        dataSource.startMonitoring()

        monitoringJob = scope.launch {
            dataSource.thermalData.collect { thermal ->
                _thermalData.value = thermal
            }
        }
    }

    fun stopMonitoring() {
        Log.i(TAG, "Request stop monitoring")
        _isMonitoring.value = false
        monitoringJob?.cancel()
        monitoringJob = null
        dataSource.stopMonitoring()
    }

    companion object {
        private const val TAG = "ThermalRepository"
    }
}
