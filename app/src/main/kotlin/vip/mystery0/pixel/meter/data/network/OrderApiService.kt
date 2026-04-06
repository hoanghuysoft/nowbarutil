package com.kakao.taxi.data.network

import com.kakao.taxi.data.model.AddOrdersRequest
import com.kakao.taxi.data.model.AddOrdersResponse
import com.kakao.taxi.data.model.GenericApiResponse
import com.kakao.taxi.data.model.OrderDetailResponse
import com.kakao.taxi.data.model.OrderListResponse
import com.kakao.taxi.data.model.UpdateOrderRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for the express.io.vn order tracking API.
 *
 * Authentication is handled via an OkHttp interceptor that injects the
 * `X-API-Key` header on every request (see [ApiKeyInterceptor]).
 */
interface OrderApiService {

    /** Fetches the full list of orders for the authenticated user. */
    @GET("api/v1/orders")
    suspend fun getOrders(): OrderListResponse

    /**
     * Fetches detailed tracking information for a single order.
     * @param code The express ID of the order (e.g. "SPXVN063578436671").
     * @param partner Optional partner hint (e.g. "SPX") for faster lookups.
     */
    @GET("api/v1/orders/{code}")
    suspend fun getOrderDetail(
        @Path("code") code: String,
        @Query("partner") partner: String? = null
    ): OrderDetailResponse

    /** Adds one or more orders to the user's tracking list. */
    @POST("api/v1/orders")
    suspend fun addOrders(@Body body: AddOrdersRequest): AddOrdersResponse

    /** Renames an existing order. */
    @PUT("api/v1/orders/{code}")
    suspend fun updateOrder(
        @Path("code") code: String,
        @Body body: UpdateOrderRequest
    ): GenericApiResponse

    /** Deletes an order from the tracking list. */
    @DELETE("api/v1/orders/{code}")
    suspend fun deleteOrder(
        @Path("code") code: String,
        @Query("partner") partner: String? = null
    ): GenericApiResponse
}
