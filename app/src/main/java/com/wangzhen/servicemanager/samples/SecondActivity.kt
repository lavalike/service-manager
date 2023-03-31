package com.wangzhen.servicemanager.samples

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wangzhen.servicemanager.ServiceManager
import com.wangzhen.servicemanager.samples.databinding.ActivitySecondBinding
import com.wangzhen.servicemanager.samples.service.Api

/**
 * SecondActivity
 * Created by wangzhen on 2023/3/7
 */
class SecondActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySecondBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            btnCall.setOnClickListener {
                callService()
            }
        }
    }

    private fun callService() {
        ServiceManager.getService(MainActivity.SERVICE_NAME)?.let {
            Toast.makeText(this, (it as Api).call(), Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Service Not Found", Toast.LENGTH_SHORT).show()
        }
    }
}