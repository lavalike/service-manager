package com.wangzhen.servicemanager;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.wangzhen.servicemanager.compat.BundleCompat;
import com.wangzhen.servicemanager.compat.ContentProviderCompat;
import com.wangzhen.servicemanager.util.ParamUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RemoteProxy
 * Created by wangzhen on 2023/3/7/007
 */
public class RemoteProxy {

    public static Object getProxyService(final String name, String iFaceClassName, ClassLoader classloader) {
        try {
            //classloader
            Class<?> clientClass = classloader.loadClass(iFaceClassName);
            return Proxy.newProxyInstance(classloader, new Class[]{clientClass}, new InvocationHandler() {
                Boolean isInProviderProcess;
                String descriptor;
                IBinder iBinder;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Bundle argsBundle = ParamUtil.wrapperParams(name, method.toGenericString(), args);
                    if (isInProviderProcess == null) {
                        prepare(argsBundle);
                    }

                    if (Boolean.TRUE.equals(isInProviderProcess)) {
                        return MethodRouter.routerToProvider(name, argsBundle);
                    } else if (descriptor != null && iBinder != null) {
                        return MethodRouter.routerToBinder(descriptor, iBinder, argsBundle);
                    } else {
                        // service process died, reboot to restore
                        Log.w("RemoteProxy", "not active，service May Died！");
                    }

                    if (!method.getReturnType().isPrimitive()) {
                        // wrapper class, return null
                        return null;
                    } else {
                        // not wrapper class, no default return value, throws RemoteException
                        throw new IllegalStateException("Service not active! Remote process may died");
                    }
                }

                private void prepare(Bundle argsBundle) throws Throwable {
                    Bundle queryResult = ContentProviderCompat.call(ServiceProvider.buildUri(), ServiceProvider.QUERY_SERVICE, name, argsBundle);
                    if (queryResult != null) {
                        isInProviderProcess = queryResult.getBoolean(ServiceProvider.QUERY_SERVICE_RESULT_IS_IN_PROVIDER_PROCESS, false);
                        iBinder = BundleCompat.getBinder(queryResult, ServiceProvider.QUERY_SERVICE_RESULT_BINDER);
                        descriptor = queryResult.getString(ServiceProvider.QUERY_SERVICE_RESULT_DESCRIPTOR);

                        if (iBinder != null) {
                            iBinder.linkToDeath(new IBinder.DeathRecipient() {
                                @Override
                                public void binderDied() {
                                    isInProviderProcess = null;
                                    iBinder = null;
                                    descriptor = null;
                                }
                            }, 0);
                        }
                    }
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
