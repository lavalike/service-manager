package com.wangzhen.servicemanager

import android.app.Application

/**
 * MainApplication
 * Created by wangzhen on 2023/3/7
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceManager.init(this)
    }
}