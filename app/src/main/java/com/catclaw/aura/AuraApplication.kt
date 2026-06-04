package com.catclaw.aura

import android.app.Application
import com.catclaw.aura.di.AppContainer
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuraApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)
        container = AppContainer(this)
        appScope.launch(Dispatchers.IO) {
            container.warmUp()
        }
    }
}
