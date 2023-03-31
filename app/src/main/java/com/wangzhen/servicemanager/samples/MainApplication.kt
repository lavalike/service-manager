package com.wangzhen.servicemanager.samples

import android.app.Application
import com.wangzhen.servicemanager.ServiceManager

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