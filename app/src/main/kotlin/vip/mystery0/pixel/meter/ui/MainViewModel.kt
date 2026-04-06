package com.kakao.taxi.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.kakao.taxi.data.model.Order
import com.kakao.taxi.data.model.OrderDetail
import com.kakao.taxi.data.repository.OrderRepository
import com.kakao.taxi.service.OrderTrackingService

class MainViewModel(
    private val application: Application,
) : AndroidViewModel(application), KoinComponent {

    private val repository: OrderRepository by inject()

    // ── State from Repository ──
    val orders = repository.orders
    val trackedOrder = repository.trackedOrder
    val trackedOrderId = repository.trackedOrderId
    val isMonitoring = repository.isMonitoring
    val isLoading = repository.isLoading
    val error = repository.error
    val apiKey = repository.apiKey

    // ── UI State ──
    private val _selectedOrderDetail = MutableStateFlow<OrderDetail?>(null)
    val selectedOrderDetail: StateFlow<OrderDetail?> = _selectedOrderDetail.asStateFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    val isHideFromRecents = repository.isHideFromRecents
    val hasNotificationPermission = MutableStateFlow(true)
    val isOledThemeEnabled = repository.isOledThemeEnabled

    init {
        checkPermissions()

        // Auto-refresh orders if API key is set
        viewModelScope.launch {
            if (repository.apiKey.value.isNotBlank()) {
                repository.refreshOrders()
            }
        }
    }

    fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission.value = ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            repository.refreshOrders()
        }
    }

    fun addOrder(code: String, name: String?) {
        viewModelScope.launch {
            repository.addOrder(code, name)
        }
    }

    fun deleteOrder(expressId: String, partner: String?) {
        viewModelScope.launch {
            repository.deleteOrder(expressId, partner)
        }
    }

    fun renameOrder(expressId: String, newName: String, partner: String?) {
        viewModelScope.launch {
            repository.updateOrder(expressId, newName, partner)
        }
    }

    /** Opens the bottom sheet with tracking detail for the given order. */
    fun showOrderDetail(order: Order) {
        viewModelScope.launch {
            _isBottomSheetVisible.value = true
            val detail = repository.fetchOrderDetail(order.expressId, order.partner)
            _selectedOrderDetail.value = detail
        }
    }

    fun hideBottomSheet() {
        _isBottomSheetVisible.value = false
        _selectedOrderDetail.value = null
    }

    /** Toggles tracking for the given order. If already tracked, untracks and stops the service. */
    fun toggleTracking(order: Order) {
        if (repository.trackedOrderId.value == order.expressId) {
            // Already tracked — untrack and stop
            repository.setTrackedOrderId("")
            stopService()
        } else {
            // Track this order and start the service
            repository.setTrackedOrderId(order.expressId)
            startService()
        }
    }

    /** Starts the foreground tracking service. */
    fun startService() {
        try {
            val intent = Intent(application, OrderTrackingService::class.java)
            application.startForegroundService(intent)
        } catch (e: Exception) {
            // Will be handled by the UI
        }
    }

    /** Stops the foreground tracking service. */
    fun stopService() {
        try {
            val intent = Intent(application, OrderTrackingService::class.java)
            application.stopService(intent)
        } catch (_: Exception) { }
    }

    fun clearError() { repository.clearError() }
}
