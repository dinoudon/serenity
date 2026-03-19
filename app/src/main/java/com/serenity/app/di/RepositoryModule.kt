package com.serenity.app.di

import com.serenity.app.data.repository.PreferencesRepositoryImpl
import com.serenity.app.data.repository.RitualRepositoryImpl
import com.serenity.app.domain.repository.PreferencesRepository
import com.serenity.app.domain.repository.RitualRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRitualRepository(impl: RitualRepositoryImpl): RitualRepository

    @Binds
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
