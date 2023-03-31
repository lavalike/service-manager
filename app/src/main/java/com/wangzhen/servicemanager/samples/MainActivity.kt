package com.wangzhen.servicemanager.samples

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.wangzhen.servicemanager.ServiceManager
import com.wangzhen.servicemanager.samples.databinding.ActivityMainBinding
import com.wangzhen.servicemanager.samples.service.Api
import com.wangzhen.servicemanager.samples.service.ApiImpl

/**
 * MainActivity
 * Created by wangzhen on 2023/3/7
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            btnPublish.setOnClickListener {
                publishService()
            }
            btnCall.setOnClickListener {
                callService()
            }
            btnLaunchProcess.setOnClickListener {
                startActivity(Intent(it.context, SecondActivity::class.java))
            }
        }
    }

    private fun callService() {
        ServiceManager.getService(SERVICE_NAME)?.let {
            Toast.makeText(this, (it as Api).call(), Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Service Not Found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun publishService() {
        ServiceManager.publishService(SERVICE_NAME, ApiImpl::class.java.name)
        Toast.makeText(this, "Service Published", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val SERVICE_NAME = "service"
    }
}