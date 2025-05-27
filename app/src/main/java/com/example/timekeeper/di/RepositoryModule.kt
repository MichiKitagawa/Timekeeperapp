package com.example.timekeeper.di

import com.example.timekeeper.data.StripeRepository
import com.example.timekeeper.data.PurchaseStateManager
import com.example.timekeeper.data.api.TimekeeperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
} 