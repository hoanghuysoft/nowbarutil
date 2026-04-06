package com.kakao.taxi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── API Response Wrappers ──

@Serializable
data class OrderListResponse(
    val status: String,
    val code: Int,
    @SerialName("user_id") val userId: String? = null,
    val username: String? = null,
    val data: OrderListData? = null,
    val message: String? = null
)

@Serializable
data class OrderListData(
    val total: Int,
    val orders: List<Order>
)

@Serializable
data class OrderDetailResponse(
    val status: String,
    val code: Int,
    @SerialName("user_id") val userId: String? = null,
    val username: String? = null,
    val data: OrderDetail? = null,
    val message: String? = null
)

@Serializable
data class GenericApiResponse(
    val status: String,
    val code: Int,
    val message: String? = null
)

// ── Core Domain Models ──

/**
 * Represents a shipping order from the list endpoint.
 */
@Serializable
data class Order(
    @SerialName("express_id") val expressId: String,
    val partner: String,
    @SerialName("item_name") val itemName: String? = null,
    val status: String,
    @SerialName("latest_status") val latestStatus: String? = null,
    @SerialName("latest_time") val latestTime: Long? = null,
    @SerialName("latest_location") val latestLocation: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

/**
 * Represents a detailed order view with full tracking history.
 */
@Serializable
data class OrderDetail(
    @SerialName("express_id") val expressId: String,
    val partner: String,
    @SerialName("item_name") val itemName: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("order_info") val orderInfo: String? = null,
    @SerialName("tracking_history") val trackingHistory: List<TrackingEvent>? = null
)

/**
 * A single event in the tracking timeline of an order.
 */
@Serializable
data class TrackingEvent(
    val status: String,
    val time: Long,
    val location: String? = null
)

// ── Request Bodies ──

@Serializable
data class AddOrdersRequest(
    val orders: List<NewOrder>
)

@Serializable
data class NewOrder(
    val code: String,
    val name: String? = null
)

@Serializable
data class UpdateOrderRequest(
    val name: String,
    val partner: String? = null
)
