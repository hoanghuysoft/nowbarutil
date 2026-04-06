package com.kakao.taxi.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that injects the `X-API-Key` header into every request.
 *
 * The API key is provided lazily via [apiKeyProvider] so that it can be
 * updated at runtime without rebuilding the OkHttp client or Retrofit instance.
 */
class ApiKeyInterceptor(
    private val apiKeyProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = apiKeyProvider()
        val request = if (apiKey.isNotBlank()) {
            chain.request().newBuilder()
                .header("X-API-Key", apiKey)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
