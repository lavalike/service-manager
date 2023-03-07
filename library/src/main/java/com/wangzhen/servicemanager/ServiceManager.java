package com.wangzhen.servicemanager;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Process;

import com.wangzhen.servicemanager.local.ServicePool;
import com.wangzhen.servicemanager.compat.BundleCompat;
import com.wangzhen.servicemanager.compat.ContentProviderCompat;

/**
 * ServiceManager
 *
 * @author: zhen51.wang
 * @date: 2023/3/2/002
 */
public class ServiceManager {

    public static final String ACTION_SERVICE_DIE_OR_CLEAR = "com.wangzhen.action.SERVICE_DIE_OR_CLEAR";

    public static Application sApplication;

    public static void init(Application application) {
        sApplication = application;

        Bundle argsBundle = new Bundle();
        int pid = Process.myPid();
        argsBundle.putInt(ServiceProvider.PID, pid);
        // publish one binder for every process
        BundleCompat.putBinder(argsBundle, ServiceProvider.BINDER, new ProcessBinder(ProcessBinder.class.getName() + "_" + pid));
        ContentProviderCompat.call(ServiceProvider.buildUri(), ServiceProvider.REPORT_BINDER, null, argsBundle);

        ServiceManager.sApplication.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // clear proxy cache when service process died or received broadcast
                ServicePool.unRegister(intent.getStringExtra(ServiceProvider.NAME));
            }
        }, new IntentFilter(ACTION_SERVICE_DIE_OR_CLEAR));
    }

    public static Object getService(String name) {
        return getService(name, ServiceManager.class.getClassLoader());
    }

    public static Object getService(String name, ClassLoader interfaceClassloader) {
        // query in the current process
        Object service = ServicePool.getService(name);
        if (service == null) {
            // query in the remote process
            Bundle bundle = ContentProviderCompat.call(ServiceProvider.buildUri(), ServiceProvider.QUERY_INTERFACE, name, null);
            if (bundle != null) {
                String interfaceClassName = bundle.getString(ServiceProvider.QUERY_INTERFACE_RESULT);
                if (interfaceClassName != null) {
                    service = RemoteProxy.getProxyService(name, interfaceClassName, interfaceClassloader);
                    //缓存Proxy到本地
                    if (service != null) {
                        ServicePool.registerInstance(name, service);
                    }
                }
            }
        }
        return service;
    }

    /**
     * publish service for other processes
     *
     * @param name      service name
     * @param className full name of impl service
     */
    public static void publishService(String name, String className) {
        publishService(name, className, ServiceManager.class.getClassLoader());
    }

    /**
     * publish service for other processes
     *
     * @param name        service name
     * @param className   full name of impl service
     * @param classloader classloader
     */
    public static void publishService(String name, final String className, final ClassLoader classloader) {
        publishService(name, new ServicePool.ClassProvider() {
            @Override
            public Object getServiceInstance() {
                try {
                    return classloader.loadClass(className).newInstance();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getInterfaceName() {
                try {
                    return classloader.loadClass(className).getInterfaces()[0].getName();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }


    /**
     * publish service for other processes
     *
     * @param name     service name
     * @param provider class provider
     */
    public static void publishService(String name, final ServicePool.ClassProvider provider) {
        // cache to local
        ServicePool.registerClass(name, provider);
        int pid = Process.myPid();
        Bundle argsBundle = new Bundle();
        argsBundle.putInt(ServiceProvider.PID, pid);

        // classLoader
        String serviceInterfaceClassName = provider.getInterfaceName();
        argsBundle.putString(ServiceProvider.INTERFACE, serviceInterfaceClassName);
        // publish to remote
        ContentProviderCompat.call(ServiceProvider.buildUri(), ServiceProvider.PUBLISH_SERVICE, name, argsBundle);

    }

    /**
     * clear all services published by current process
     */
    public static void unPublishAllService() {
        int pid = Process.myPid();
        Bundle argsBundle = new Bundle();
        argsBundle.putInt(ServiceProvider.PID, pid);
        ContentProviderCompat.call(ServiceProvider.buildUri(), ServiceProvider.UNPUBLISH_SERVICE, null, argsBundle);
    }

    /**
     * clear specific service published by current process
     *
     * @param name service name
     */
    public static void unPublishService(String name) {
        int pid = Process.myPid();
        Bundle argsBundle = new Bundle();
        argsBundle.putInt(ServiceProvider.PID, pid);
        argsBundle.putString(ServiceProvider.NAME, name);
        ContentProviderCompat.call(ServiceProvider.buildUri(), ServiceProvider.UNPUBLISH_SERVICE, null, argsBundle);
    }

}
