package com.trafficanalyzer

import android.app.Application
import com.trafficanalyzer.di.AppContainer

/**
 * TrafficAnalyzerApp — Application class that holds the manual DI container.
 * AppContainer is a singleton object holding all shared dependencies.
 */
class TrafficAnalyzerApp : Application() {
    /** Application-wide dependency container, accessible from anywhere via app reference */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
