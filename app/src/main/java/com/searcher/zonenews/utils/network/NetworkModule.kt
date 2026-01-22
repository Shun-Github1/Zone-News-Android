package com.searcher.zonenews.utils.network


import android.content.Context
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import java.security.SecureRandom

import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.SharedPreferenceUtils
import com.searcher.zonenews.utils.network.exception.NetworkResponseAdapterFactory
import com.searcher.zonenews.utils.network.PersistentCookieJar

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.searcher.zonenews.entry.ArticleDetailEntry
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Custom Trust Manager to handle SSL certificate issues
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    // SSL Context that accepts all certificates
    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    // Cookie jar instance - shared across the app
    @Provides
    @Singleton
    fun provideCookieJar(@ApplicationContext context: Context): PersistentCookieJar {
        return PersistentCookieJar(context)
    }

    // 普通用户的 OkHttpClient
    @Provides
    @Singleton
    @Named("user")
    fun provideUserOkHttpClient(
        @ApplicationContext context: Context,
        cookieJar: PersistentCookieJar
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Accept all hostnames
            .cookieJar(cookieJar) // Enable cookie-based authentication
            // Add CSRF token interceptor for POST requests
            .addInterceptor { chain ->
                val request = chain.request()
                // Only add CSRF token for POST, PUT, DELETE requests
                if (request.method in listOf("POST", "PUT", "DELETE")) {
                    val csrfToken = cookieJar.getCsrfToken()
                    if (!csrfToken.isNullOrEmpty()) {
                        val newRequest = request.newBuilder()
                            .addHeader("X-CSRF-Token", csrfToken)
                            .build()
                        chain.proceed(newRequest)
                    } else {
                        chain.proceed(request)
                    }
                } else {
                    chain.proceed(request)
                }
            }
            .addInterceptor(CustomLoggingInterceptor())
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // 商家的 OkHttpClient
    @Provides
    @Singleton
    @Named("merchant")
    fun provideMerchantOkHttpClient(@ApplicationContext context: Context): OkHttpClient {

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Accept all hostnames
            .addInterceptor { chain ->
                val token = SharedPreferenceUtils.getString(context,"token")
                val rtoken = token ?: ""
                val request = chain.request().newBuilder()
                    .addHeader("token", rtoken) // 添加 rtoken
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // Custom Gson instance with PublisherStance deserializer
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(
                  ArticleDetailEntry.DataDTO.PublisherStanceDTO::class.java,
                PublisherStanceDeserializer()
            )
            .create()
    }

    // 普通用户的 Retrofit
    @Provides
    @Singleton
    @Named("user")
    fun provideUserRetrofit(
        @Named("user") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.COMMON_URL)
            .client(okHttpClient)
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // 商家的 Retrofit
    @Provides
    @Singleton
    @Named("merchant")
    fun provideMerchantRetrofit(
        @Named("merchant") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.COMMON_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // 普通用户的 ApiService
    @Provides
    @Singleton
    fun provideApiService(@Named("user") retrofit: Retrofit): AppHttpService {
        return retrofit.create(AppHttpService::class.java)
    }


}
