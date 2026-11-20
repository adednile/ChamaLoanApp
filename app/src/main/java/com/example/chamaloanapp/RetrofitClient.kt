package com.example.chamaloanapp

import android.os.Build
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Singleton networking client managing Retrofit and OkHttpClient configurations.
 * 
 * Auto-detects whether the app is running on an Android Studio Emulator
 * (http://10.0.2.2:8000/) or a physical Android phone via USB/Wi-Fi (http://127.0.0.1:8000/).
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"

    // Default loopback URL for standard Android Studio Emulator (AVD)
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8000/"

    // Default URL for physical device connected via USB with "adb reverse tcp:8000 tcp:8000"
    const val PHYSICAL_USB_BASE_URL = "http://127.0.0.1:8000/"

    /**
     * Checks if the device running the app is an Android Emulator or a real physical phone.
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Returns the recommended base URL based on device environment.
     */
    fun getDefaultBaseUrl(): String {
        return if (isEmulator()) EMULATOR_BASE_URL else PHYSICAL_USB_BASE_URL
    }

    @Volatile
    private var currentBaseUrl: String = getDefaultBaseUrl()

    @Volatile
    private var retrofitInstance: Retrofit? = null

    @Volatile
    private var serviceInstance: ChamaApiService? = null

    /**
     * Custom logging interceptor to trace HTTP requests and responses in Logcat.
     */
    private val loggingInterceptor = Interceptor { chain: Interceptor.Chain ->
        val request = chain.request()
        val startTime = System.nanoTime()
        Log.d(TAG, "--> ${request.method()} ${request.url()}")

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: IOException) {
            Log.e(TAG, "<-- HTTP FAILED: ${e.message}", e)
            throw e
        }

        val tookMs = (System.nanoTime() - startTime) / 1e6
        Log.d(TAG, "<-- ${response.code()} ${response.message()} (${String.format("%.1f", tookMs)}ms) for ${request.url()}")
        response
    }

    /**
     * Fully configured OkHttpClient with connection timeouts and logging.
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Returns the singleton Retrofit instance. Re-instantiated if baseUrl changes.
     */
    @Synchronized
    fun getRetrofit(): Retrofit {
        val existing = retrofitInstance
        if (existing != null) return existing

        val newRetrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofitInstance = newRetrofit
        return newRetrofit
    }

    /**
     * Singleton instance of the ChamaApiService.
     */
    val apiService: ChamaApiService
        @Synchronized
        get() {
            var service = serviceInstance
            if (service == null) {
                service = getRetrofit().create(ChamaApiService::class.java)
                serviceInstance = service
            }
            return service
        }

    /**
     * Allows dynamically updating the base URL (e.g. For physical device LAN IP or USB reverse).
     */
    @Synchronized
    fun setBaseUrl(newUrl: String) {
        val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (currentBaseUrl != formattedUrl) {
            currentBaseUrl = formattedUrl
            retrofitInstance = null
            serviceInstance = null
            Log.i(TAG, "Retrofit base URL updated to: $currentBaseUrl")
        }
    }

    fun getBaseUrl(): String = currentBaseUrl
}
