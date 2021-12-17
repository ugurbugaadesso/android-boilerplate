package com.adesso.movee.internal.injection.module

import android.content.Context
import com.adesso.movee.BuildConfig
import com.adesso.movee.data.remote.api.MovieService
import com.adesso.movee.internal.util.DateAdapter
import com.adesso.movee.internal.util.ImageJsonAdapter
import com.adesso.movee.internal.util.NetworkStateHolder
import com.adesso.movee.internal.util.api.ApiKeyInterceptor
import com.adesso.movee.internal.util.api.ErrorHandlingInterceptor
import com.adesso.movee.internal.util.api.RetryAfterInterceptor
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.moczul.ok2curl.CurlInterceptor
import com.serjltt.moshi.adapters.Wrapped
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
internal class NetworkModule {

    companion object {
        private const val CLIENT_TIME_OUT_SEC = 30L
    }

    @Provides
    @Singleton
    internal fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = if (BuildConfig.ENABLE_LOG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return loggingInterceptor
    }

    @Provides
    @Singleton
    internal fun provideCurlInterceptor(): CurlInterceptor {
        return CurlInterceptor { message -> if (BuildConfig.ENABLE_LOG) println(message) }
    }

    @Provides
    @Singleton
    fun provideChuckerInterceptor(@ApplicationContext context: Context) =
        ChuckerInterceptor.Builder(context).build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        curlInterceptor: CurlInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
        moshi: Moshi
    ): OkHttpClient {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(CLIENT_TIME_OUT_SEC, TimeUnit.SECONDS)
            .readTimeout(CLIENT_TIME_OUT_SEC, TimeUnit.SECONDS)
            .addInterceptor(ApiKeyInterceptor())
            .addInterceptor(chuckerInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(curlInterceptor)
            .addInterceptor(ErrorHandlingInterceptor(NetworkStateHolder, moshi))
            .addInterceptor(RetryAfterInterceptor())

        return httpClient.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: Lazy<OkHttpClient>, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { client.get().newCall(it) }
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieService(retrofit: Retrofit): MovieService {
        return retrofit.create(MovieService::class.java)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(ImageJsonAdapter())
            .add(DateAdapter())
            .add(Wrapped.ADAPTER_FACTORY)
            .build()
    }
}
