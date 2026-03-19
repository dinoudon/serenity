package com.serenity.app.di

import android.content.Context
import androidx.room.Room
import com.serenity.app.data.local.RitualDao
import com.serenity.app.data.local.SerenityDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SerenityDatabase {
        return Room.databaseBuilder(
            context,
            SerenityDatabase::class.java,
            "serenity_database"
        ).build()
    }

    @Provides
    fun provideRitualDao(database: SerenityDatabase): RitualDao {
        return database.ritualDao()
    }
}
