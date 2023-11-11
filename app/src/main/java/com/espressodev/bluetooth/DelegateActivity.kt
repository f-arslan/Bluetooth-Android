package com.espressodev.bluetooth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KProperty

class DelegateActivity : ComponentActivity(),
    AnalyticsLogger by AnalyticsLoggerImpl(),
    DeepLinkHandler by DeepLinkHandlerImpl() {

    val number by MyLazy {
        println("Lazy init")
        4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLifecycleOwner(this)
        println(number)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(this, intent)
    }
}

class MyLazy<out T : Any>(
    private val initialize: () -> T
) {
    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return if (value == null) {
            value = initialize()
            value!!
        } else value!!
    }
}

interface DeepLinkHandler {
    fun handleDeepLink(activity: ComponentActivity, intent: Intent?)
}

class DeepLinkHandlerImpl : DeepLinkHandler {
    override fun handleDeepLink(activity: ComponentActivity, intent: Intent?) {

    }
}

interface AnalyticsLogger {
    fun registerLifecycleOwner(owner: LifecycleOwner)
}

class AnalyticsLoggerImpl : AnalyticsLogger, LifecycleEventObserver {
    override fun registerLifecycleOwner(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> println("User opened the app")
            Lifecycle.Event.ON_PAUSE -> println("User left the app")
            else -> Unit
        }
    }
}