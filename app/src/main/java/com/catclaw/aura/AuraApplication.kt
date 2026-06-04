package com.catclaw.aura

import android.app.Application
import com.catclaw.aura.di.AppContainer

class AuraApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
