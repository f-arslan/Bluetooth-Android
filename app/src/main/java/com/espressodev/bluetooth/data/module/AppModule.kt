package com.espressodev.bluetooth.data.module

import android.content.Context
import com.espressodev.bluetooth.data.BTController
import com.espressodev.bluetooth.data.BTDataTransferService
import com.espressodev.bluetooth.data.impl.BTControllerImpl
import com.espressodev.bluetooth.data.impl.BTDataTransferServiceImpl
import dagger.Binds
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
    fun provideBLEController(@ApplicationContext context: Context): BTController {
        return BTControllerImpl(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    abstract fun provideBTDataTransferService(impl: BTDataTransferServiceImpl): BTDataTransferService
}
