package com.example.timekeeper.di

import android.content.Context
import com.example.timekeeper.data.AppUsageRepository
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.data.PurchaseStateManager
import com.example.timekeeper.data.StripeRepository
import com.example.timekeeper.data.api.TimekeeperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideStripeRepository(
        apiService: TimekeeperApiService,
        purchaseStateManager: PurchaseStateManager
    ): StripeRepository {
        return StripeRepository(apiService, purchaseStateManager)
    }

    @Provides
    @Singleton
    fun providePurchaseStateManager(@ApplicationContext context: Context): PurchaseStateManager {
        return PurchaseStateManager(context)
    }

    @Provides
    @Singleton
    fun provideAppUsageRepository(@ApplicationContext context: Context): AppUsageRepository {
        return AppUsageRepository(context)
    }

    @Provides
    @Singleton
    fun provideMonitoredAppRepository(@ApplicationContext context: Context): MonitoredAppRepository {
        return MonitoredAppRepository(context)
    }
} 