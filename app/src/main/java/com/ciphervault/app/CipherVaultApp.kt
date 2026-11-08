package com.ciphervault.app

import android.app.Application

class CipherVaultApp : Application() {
    lateinit var container: AppContainer
        private set

    /** 窄接口视图；消费者应优先使用 [appServices]。 */
    val services: AppServices
        get() = container

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
