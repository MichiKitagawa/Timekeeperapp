package com.example.timekeeper.di

import com.example.timekeeper.data.AppUsageRepository
import com.example.timekeeper.data.MonitoredAppRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun appUsageRepository(): AppUsageRepository
    fun monitoredAppRepository(): MonitoredAppRepository
} 