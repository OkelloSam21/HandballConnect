package com.example.handballconnect.di

import android.content.Context
import com.example.handballconnect.data.repository.MessageRepository
import com.example.handballconnect.data.repository.PostRepository
import com.example.handballconnect.data.repository.TacticsRepository
import com.example.handballconnect.data.repository.UserRepository
import com.example.handballconnect.data.storage.ImageStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideImageStorageManager(
        @ApplicationContext context: Context
    ): ImageStorageManager {
        return ImageStorageManager(context)
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(
        imageStorageManager: ImageStorageManager
    ): UserRepository {
        return UserRepository(imageStorageManager)
    }
    
    @Provides
    @Singleton
    fun providePostRepository(
        userRepository: UserRepository,
        imageStorageManager: ImageStorageManager
    ): PostRepository {
        return PostRepository(userRepository, imageStorageManager)
    }
    
    @Provides
    @Singleton
    fun provideMessageRepository(
        userRepository: UserRepository
    ): MessageRepository {
        return MessageRepository(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideTacticsRepository(
        userRepository: UserRepository
    ): TacticsRepository {
        return TacticsRepository(userRepository)
    }
}