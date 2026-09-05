package com.armadio.core.database.di

import android.content.Context
import androidx.room.Room
import com.armadio.core.database.AppDatabase
import com.armadio.core.database.dao.GarmentDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Deliberately NO fallbackToDestructiveMigration() — constraint #7.
            // Migrations are mandatory and tested with MigrationTestHelper.
            .build()

    @Provides
    fun provideGarmentDao(database: AppDatabase): GarmentDao = database.garmentDao()
}
