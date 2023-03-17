# service manager

轻量级多进程框架

### 添加仓库

``` groovy
maven {
    url 'https://jitpack.io'
}
```

### 依赖组件

``` groovy
implementation 'com.github.lavalike:service-manager:1.0.0'
```

### 初始化

在Application中初始化：

``` kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 每个进程都要初始化
        ServiceManager.init(this)
    }
}
```

### 定义服务

分别定义服务接口与实现类

``` java
interface Api {
    fun call(): String
}

class ApiImpl : Api {
    override fun call(): String = "Hello Api, Process: ${android.os.Process.myPid()}"
}
```

### 发布服务

在任意进程中发布服务

``` kotlin
ServiceManager.publishService(SERVICE_NAME, ApiImpl::class.java.name)
```

### 使用服务

在任意进程中获取服务并调用

``` kotlin
ServiceManager.getService(SERVICE_NAME)?.let {
    Toast.makeText(this, (it as Api).call(), Toast.LENGTH_SHORT).show()
}
```


