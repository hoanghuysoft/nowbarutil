package com.kakao.taxi.di

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kakao.taxi.data.network.ApiKeyInterceptor
import com.kakao.taxi.data.network.OrderApiService
import com.kakao.taxi.data.repository.DataStoreRepository
import com.kakao.taxi.data.repository.OrderRepository
import com.kakao.taxi.data.repository.dataStore
import com.kakao.taxi.service.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

val appModule = module {
    single { androidContext().getSystemService(Context.POWER_SERVICE) as PowerManager }
    single { androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    single { DataStoreRepository(androidContext().dataStore) }

    // Network — uses a lazy API key provider to avoid circular DI.
    // The interceptor reads the API key directly from DataStoreRepository on each request.
    single {
        val dataStoreRepo = get<DataStoreRepository>()
        val interceptor = ApiKeyInterceptor {
            // Read the latest API key synchronously from DataStore.
            // This runs on OkHttp's dispatcher thread, not the main thread.
            runBlocking { dataStoreRepo.apiKey.first() }
        }
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    single {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://express.io.vn/")
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OrderApiService::class.java)
    }

    single { OrderRepository(get(), get()) }

    factory { NotificationHelper(androidContext()) }
}
