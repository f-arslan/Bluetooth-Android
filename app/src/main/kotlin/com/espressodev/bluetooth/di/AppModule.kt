package com.espressodev.bluetooth.di

import android.content.Context
import com.espressodev.bluetooth.TicTacToeApp
import com.espressodev.bluetooth.domain.model.TicTacToe
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
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
    fun provideConnectionsClients(@ApplicationContext context: Context): ConnectionsClient =
        Nearby.getConnectionsClient(context)

    @Provides
    @Singleton
    fun provideGame() = TicTacToe()
}