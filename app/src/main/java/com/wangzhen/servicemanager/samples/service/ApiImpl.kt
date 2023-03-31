package com.wangzhen.servicemanager.samples.service

/**
 * ApiImpl
 * Created by wangzhen on 2023/3/7
 */
class ApiImpl : Api {
    override fun call(): String = "Hello Api, Process: ${android.os.Process.myPid()}"
}