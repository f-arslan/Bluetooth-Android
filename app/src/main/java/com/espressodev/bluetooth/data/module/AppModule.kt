package com.espressodev.bluetooth.data.module

import android.content.Context
import com.espressodev.bluetooth.data.BluetoothController
import com.espressodev.bluetooth.data.impl.BluetoothControllerImpl
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
    fun provideBLEController(@ApplicationContext context: Context): BluetoothController {
        return BluetoothControllerImpl(context)
    }
}
