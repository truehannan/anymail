package com.shrivatsav.monomail.di

import com.shrivatsav.monomail.core.network.remote.resend.ResendApiService
import com.shrivatsav.monomail.core.network.provider.resend.ResendProvider
import com.shrivatsav.monomail.core.data.repository.resend.ResendRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ResendModule {

    private const val RESEND_BASE_URL = "https://api.resend.com/"

    @Provides
    @Singleton
    fun provideResendOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val newRequest = request.newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideResendRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(RESEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideResendApiService(retrofit: Retrofit): ResendApiService {
        return retrofit.create(ResendApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideResendProvider(apiService: ResendApiService): ResendProvider {
        return ResendProvider(apiService)
    }

    @Provides
    @Singleton
    fun provideResendRepository(resendProvider: ResendProvider): ResendRepository {
        return ResendRepository(resendProvider)
    }
}