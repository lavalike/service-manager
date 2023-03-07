package com.wangzhen.servicemanager;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.wangzhen.servicemanager.local.ServicePool;
import com.wangzhen.servicemanager.compat.BundleCompat;

import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Using Content Provider to Realize Synchronous Cross-process Call, If the process of content provider exits,
 * Binder and service information registered by other service processes will be lost
 *
 * @author: zhen51.wang
 * @date: 2023/3/2/002
 */
public class ServiceProvider extends ContentProvider {
    public static final String REPORT_BINDER = "report_binder";
    public static final String PUBLISH_SERVICE = "publish_service";
    public static final String PUBLISH_SERVICE_BINDER = "publish_service_binder";
    public static final String UNPUBLISH_SERVICE = "unpublish_service";
    public static final String CALL_SERVICE = "call_service";
    public static final String QUERY_SERVICE = "query_service";
    public static final String QUERY_SERVICE_RESULT_IS_IN_PROVIDER_PROCESS = "query_service_result_is_in_provider_process";
    public static final String QUERY_SERVICE_RESULT_BINDER = "query_service_result_binder";
    public static final String QUERY_SERVICE_RESULT_DESCRIPTOR = "query_service_result_descriptor";
    public static final String QUERY_INTERFACE = "query_interface";
    public static final String QUERY_INTERFACE_RESULT = "query_interface_result";

    public static final String PID = "pid";
    public static final String BINDER = "binder";
    public static final String NAME = "name";
    public static final String INTERFACE = "interface";

    private static Uri CONTENT_URI;

    // Service name: Process ID
    private static final ConcurrentHashMap<String, Recorder> allServiceList = new ConcurrentHashMap<>();
    // Process ID: Process Binder
    private static final ConcurrentHashMap<Integer, IBinder> processBinder = new ConcurrentHashMap<>();

    public static Uri buildUri() {
        if (CONTENT_URI == null) {
            CONTENT_URI = Uri.parse("content://" + ServiceManager.sApplication.getPackageName() + ".svcmgr/call");
        }
        return CONTENT_URI;
    }

    @Override
    public Bundle call(String method, String serviceName, Bundle extras) {
        if (Build.VERSION.SDK_INT >= 19) {
            Log.d("call", "callingPackage = " + getCallingPackage());
        }
        Log.d("call", "Thread : id = " + Thread.currentThread().getId() + ", name = " + Thread.currentThread().getName() + ", method = " + method + ", arg = " + serviceName);

        Bundle bundle;
        Recorder recorder;
        switch (method) {
            case REPORT_BINDER: {
                final int pid = extras.getInt(PID);
                IBinder iBinder = BundleCompat.getBinder(extras, BINDER);
                processBinder.put(pid, iBinder);
                try {
                    iBinder.linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            removeAllRecorderForPid(pid);
                        }
                    }, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    processBinder.remove(pid);
                }
                break;
            }
            case PUBLISH_SERVICE: {
                int pid = extras.getInt(PID);
                String interfaceClass = extras.getString(INTERFACE);
                IBinder binder = processBinder.get(pid);
                if (binder != null && binder.isBinderAlive()) {
                    recorder = new Recorder();
                    recorder.pid = pid;
                    recorder.interfaceClass = interfaceClass;
                    allServiceList.put(serviceName, recorder);
                } else {
                    allServiceList.remove(pid);
                }
                return null;
            }
            case UNPUBLISH_SERVICE: {
                int pid = extras.getInt(PID);
                String name = extras.getString(NAME);
                if (TextUtils.isEmpty(name)) {
                    removeAllRecorderForPid(pid);
                } else {
                    allServiceList.remove(name);
                    notifyClient(name);
                }
                return null;
            }
            case CALL_SERVICE:
                return MethodRouter.routerToInstance(extras);
            case QUERY_INTERFACE:
                bundle = new Bundle();
                recorder = allServiceList.get(serviceName);
                if (recorder != null) {
                    bundle.putString(QUERY_INTERFACE_RESULT, recorder.interfaceClass);
                }
                return bundle;
            case QUERY_SERVICE:
                if (allServiceList.containsKey(serviceName)) {
                    Object instance = ServicePool.getService(serviceName);
                    bundle = new Bundle();
                    if (instance != null && !Proxy.isProxyClass(instance.getClass())) {
                        bundle.putBoolean(QUERY_SERVICE_RESULT_IS_IN_PROVIDER_PROCESS, true);
                        return bundle;
                    } else {
                        recorder = allServiceList.get(serviceName);
                        if (recorder != null) {
                            IBinder iBinder = processBinder.get(recorder.pid);
                            if (iBinder != null && iBinder.isBinderAlive()) {
                                bundle.putBoolean(QUERY_SERVICE_RESULT_IS_IN_PROVIDER_PROCESS, false);
                                bundle.putString(QUERY_SERVICE_RESULT_DESCRIPTOR, ProcessBinder.class.getName() + "_" + recorder.pid);
                                BundleCompat.putBinder(bundle, QUERY_SERVICE_RESULT_BINDER, iBinder);
                                return bundle;
                            }
                        }
                        return null;
                    }
                }

                break;
        }
        return null;
    }

    private void removeAllRecorderForPid(int pid) {
        Log.w("ServiceProvider", "remove all service recorder for pid" + pid);
        // if the service provider died, or actively notify to clean services, clean service registry first, then notify all clients to clean local cache
        processBinder.remove(pid);
        Iterator<Map.Entry<String, Recorder>> iterator = allServiceList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Recorder> entry = iterator.next();
            if (entry.getValue().pid.equals(pid)) {
                iterator.remove();
                notifyClient(entry.getKey());
            }
        }
    }

    private void notifyClient(String name) {
        // notify clients holding services to clean cache
        Intent intent = new Intent(ServiceManager.ACTION_SERVICE_DIE_OR_CLEAR);
        intent.putExtra(NAME, name);
        ServiceManager.sApplication.sendBroadcast(intent);
    }

    public static class Recorder {
        public Integer pid;
        public String interfaceClass;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //doNothing
        return null;
    }

    @Override
    public String getType(Uri uri) {
        //doNothing
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //doNothing
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //doNothing
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        //doNothing
        return 0;
    }

}
