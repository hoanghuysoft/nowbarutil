package com.kakao.taxi.data.repository

import android.util.Log
import com.kakao.taxi.data.model.AddOrdersRequest
import com.kakao.taxi.data.model.NewOrder
import com.kakao.taxi.data.model.Order
import com.kakao.taxi.data.model.OrderDetail
import com.kakao.taxi.data.model.TrackingEvent
import com.kakao.taxi.data.model.UpdateOrderRequest
import com.kakao.taxi.data.network.OrderApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent

/**
 * Central repository for order tracking.
 *
 * Responsibilities:
 * - CRUD operations on orders via [OrderApiService]
 * - Polling the tracked order for real-time status updates
 * - Exposing UI/notification preferences from [DataStoreRepository]
 */
class OrderRepository(
    private val apiService: OrderApiService,
    private val dataStoreRepository: DataStoreRepository,
) : KoinComponent {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var trackingJob: Job? = null

    // ── Tracking State ──

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _trackedOrder = MutableStateFlow<OrderDetail?>(null)
    val trackedOrder: StateFlow<OrderDetail?> = _trackedOrder.asStateFlow()

    private val _trackedOrderSummary = MutableStateFlow<Order?>(null)
    val trackedOrderSummary: StateFlow<Order?> = _trackedOrderSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Preferences ──

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _trackedOrderId = MutableStateFlow("")
    val trackedOrderId: StateFlow<String> = _trackedOrderId.asStateFlow()

    private val _pollingInterval = MutableStateFlow(30000L)
    val pollingInterval: StateFlow<Long> = _pollingInterval.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(true)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _isLiveUpdateEnabled = MutableStateFlow(true)
    val isLiveUpdateEnabled: StateFlow<Boolean> = _isLiveUpdateEnabled.asStateFlow()

    private val _isBlankNotificationEnabled = MutableStateFlow(false)
    val isBlankNotificationEnabled: StateFlow<Boolean> = _isBlankNotificationEnabled.asStateFlow()

    private val _notificationTextSize = MutableStateFlow(0.60f)
    val notificationTextSize: StateFlow<Float> = _notificationTextSize.asStateFlow()

    private val _notificationUnitSize = MutableStateFlow(0.45f)
    val notificationUnitSize: StateFlow<Float> = _notificationUnitSize.asStateFlow()

    private val _notificationUseCustomColor = MutableStateFlow(false)
    val notificationUseCustomColor: StateFlow<Boolean> = _notificationUseCustomColor.asStateFlow()

    private val _notificationColor = MutableStateFlow(0xFF888888.toInt())
    val notificationColor: StateFlow<Int> = _notificationColor.asStateFlow()

    private val _isHideFromRecents = MutableStateFlow(false)
    val isHideFromRecents: StateFlow<Boolean> = _isHideFromRecents.asStateFlow()

    private val _isAutoStartServiceEnabled = MutableStateFlow(false)
    val isAutoStartServiceEnabled: StateFlow<Boolean> = _isAutoStartServiceEnabled.asStateFlow()

    private val _isOledThemeEnabled = MutableStateFlow(false)
    val isOledThemeEnabled: StateFlow<Boolean> = _isOledThemeEnabled.asStateFlow()

    init {
        // Load initial values synchronously to avoid flickering
        runBlocking {
            dataStoreRepository.allPreferences.first().let { prefs ->
                _apiKey.value = prefs[DataStoreRepository.KEY_API_KEY] ?: ""
                _trackedOrderId.value = prefs[DataStoreRepository.KEY_TRACKED_ORDER_ID] ?: ""
                _pollingInterval.value = prefs[DataStoreRepository.KEY_POLLING_INTERVAL] ?: 30000L
                _isNotificationEnabled.value = prefs[DataStoreRepository.KEY_NOTIFICATION_ENABLED] ?: true
                _isLiveUpdateEnabled.value = prefs[DataStoreRepository.KEY_LIVE_UPDATE] ?: true
                _isBlankNotificationEnabled.value = prefs[DataStoreRepository.KEY_BLANK_NOTIFICATION] ?: false
                _notificationTextSize.value = prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_SIZE] ?: 0.60f
                _notificationUnitSize.value = prefs[DataStoreRepository.KEY_NOTIFICATION_UNIT_SIZE] ?: 0.45f
                _notificationUseCustomColor.value = prefs[DataStoreRepository.KEY_NOTIFICATION_USE_CUSTOM_COLOR] ?: false
                _notificationColor.value = prefs[DataStoreRepository.KEY_NOTIFICATION_COLOR] ?: 0xFF888888.toInt()
                _isHideFromRecents.value = prefs[DataStoreRepository.KEY_HIDE_FROM_RECENTS] ?: false
                _isAutoStartServiceEnabled.value = prefs[DataStoreRepository.KEY_AUTO_START_SERVICE] ?: false
                _isOledThemeEnabled.value = prefs[DataStoreRepository.KEY_OLED_THEME] ?: false
            }
        }

        // Observe preference changes
        scope.launch { dataStoreRepository.apiKey.collect { _apiKey.value = it } }
        scope.launch { dataStoreRepository.trackedOrderId.collect { _trackedOrderId.value = it } }
        scope.launch { dataStoreRepository.pollingInterval.collect { _pollingInterval.value = it } }
        scope.launch { dataStoreRepository.isNotificationEnabled.collect { _isNotificationEnabled.value = it } }
        scope.launch { dataStoreRepository.isLiveUpdateEnabled.collect { _isLiveUpdateEnabled.value = it } }
        scope.launch { dataStoreRepository.isBlankNotificationEnabled.collect { _isBlankNotificationEnabled.value = it } }
        scope.launch { dataStoreRepository.notificationTextSize.collect { _notificationTextSize.value = it } }
        scope.launch { dataStoreRepository.notificationUnitSize.collect { _notificationUnitSize.value = it } }
        scope.launch { dataStoreRepository.notificationUseCustomColor.collect { _notificationUseCustomColor.value = it } }
        scope.launch { dataStoreRepository.notificationColor.collect { _notificationColor.value = it } }
        scope.launch { dataStoreRepository.isHideFromRecents.collect { _isHideFromRecents.value = it } }
        scope.launch { dataStoreRepository.isAutoStartServiceEnabled.collect { _isAutoStartServiceEnabled.value = it } }
        scope.launch { dataStoreRepository.isOledThemeEnabled.collect { _isOledThemeEnabled.value = it } }
    }

    // ── Order CRUD ──

    /** Fetches the full order list from the API. */
    suspend fun refreshOrders() {
        if (_apiKey.value.isBlank()) {
            _error.value = "API key not configured"
            return
        }
        _isLoading.value = true
        _error.value = null
        try {
            val response = apiService.getOrders()
            if (response.status == "ok" && response.data != null) {
                _orders.value = response.data.orders
            } else {
                _error.value = response.message ?: "Failed to fetch orders"
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshOrders failed", e)
            _error.value = e.message ?: "Network error"
        } finally {
            _isLoading.value = false
        }
    }

    /** Fetches detailed tracking info for a specific order. */
    suspend fun fetchOrderDetail(expressId: String, partner: String? = null): OrderDetail? {
        return try {
            val response = apiService.getOrderDetail(expressId, partner)
            if (response.status == "ok" && response.data != null) {
                response.data
            } else {
                _error.value = response.message ?: "Failed to fetch order detail"
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchOrderDetail failed", e)
            _error.value = e.message ?: "Network error"
            null
        }
    }

    /** Adds a new order to the tracking list. */
    suspend fun addOrder(code: String, name: String? = null) {
        try {
            val request = AddOrdersRequest(orders = listOf(NewOrder(code, name)))
            val response = apiService.addOrders(request)
            if (response.status == "ok") {
                refreshOrders()
            } else {
                _error.value = response.message ?: "Failed to add order"
            }
        } catch (e: Exception) {
            Log.e(TAG, "addOrder failed", e)
            _error.value = e.message ?: "Network error"
        }
    }

    /** Renames an order. */
    suspend fun updateOrder(expressId: String, name: String, partner: String? = null) {
        try {
            val request = UpdateOrderRequest(name, partner)
            val response = apiService.updateOrder(expressId, request)
            if (response.status == "ok") {
                refreshOrders()
            } else {
                _error.value = response.message ?: "Failed to update order"
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateOrder failed", e)
            _error.value = e.message ?: "Network error"
        }
    }

    /** Deletes an order from the tracking list. */
    suspend fun deleteOrder(expressId: String, partner: String? = null) {
        try {
            val response = apiService.deleteOrder(expressId, partner)
            if (response.status == "ok") {
                // If the deleted order was being tracked, stop tracking
                if (_trackedOrderId.value == expressId) {
                    setTrackedOrderId("")
                }
                refreshOrders()
            } else {
                _error.value = response.message ?: "Failed to delete order"
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteOrder failed", e)
            _error.value = e.message ?: "Network error"
        }
    }

    // ── Tracking (Polling Loop) ──

    /** Starts the polling loop for the currently tracked order. */
    fun startTracking() {
        val orderId = _trackedOrderId.value
        if (orderId.isBlank()) {
            Log.w(TAG, "No order selected for tracking")
            return
        }

        _isMonitoring.value = true
        trackingJob?.cancel()

        // Find the partner from our cached order list for faster API lookups
        val partner = _orders.value.find { it.expressId == orderId }?.partner

        trackingJob = scope.launch {
            while (true) {
                try {
                    val detail = fetchOrderDetail(orderId, partner)
                    if (detail != null) {
                        _trackedOrder.value = detail
                        // Also update the summary from the order list matching this ID
                        _trackedOrderSummary.value = _orders.value.find { it.expressId == orderId }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Tracking poll failed", e)
                }
                delay(_pollingInterval.value.coerceAtLeast(10_000L))
            }
        }
        Log.i(TAG, "Tracking started for $orderId (interval=${_pollingInterval.value}ms)")
    }

    /** Stops the polling loop. */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _isMonitoring.value = false
        Log.i(TAG, "Tracking stopped")
    }

    fun clearError() { _error.value = null }

    // ── Preference Setters ──

    fun setApiKey(key: String) { scope.launch { dataStoreRepository.setApiKey(key) } }
    fun setTrackedOrderId(id: String) { scope.launch { dataStoreRepository.setTrackedOrderId(id) } }
    fun setPollingInterval(interval: Long) { scope.launch { dataStoreRepository.setPollingInterval(interval.coerceAtLeast(10_000L)) } }
    fun setNotificationEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setNotificationEnabled(enabled) } }
    fun setLiveUpdateEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setLiveUpdateEnabled(enabled) } }
    fun setBlankNotificationEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setBlankNotificationEnabled(enabled) } }
    fun setNotificationTextSize(size: Float) { scope.launch { dataStoreRepository.setNotificationTextSize(size) } }
    fun setNotificationUnitSize(size: Float) { scope.launch { dataStoreRepository.setNotificationUnitSize(size) } }
    fun setNotificationUseCustomColor(useCustom: Boolean) { scope.launch { dataStoreRepository.setNotificationUseCustomColor(useCustom) } }
    fun setNotificationColor(color: Int) { scope.launch { dataStoreRepository.setNotificationColor(color) } }
    fun setHideFromRecents(hide: Boolean) { scope.launch { dataStoreRepository.setHideFromRecents(hide) } }
    fun setAutoStartServiceEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setAutoStartServiceEnabled(enabled) } }
    fun setOledThemeEnabled(enabled: Boolean) { scope.launch { dataStoreRepository.setOledThemeEnabled(enabled) } }

    companion object {
        private const val TAG = "OrderRepository"
    }
}
