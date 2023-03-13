package com.wangzhen.servicemanager.compat;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.wangzhen.servicemanager.ServiceManager;
import com.wangzhen.servicemanager.util.ReflectUtil;

/**
 * ContentProviderCompat
 * Created by wangzhen on 2023/3/7/007
 */
public class ContentProviderCompat {

    public static Bundle call(Uri uri, String method, String arg, Bundle extras) {
        ContentResolver resolver = ServiceManager.sApplication.getContentResolver();
        if (Build.VERSION.SDK_INT >= 11) {
            return resolver.call(uri, method, arg, extras);
        } else {
            ContentProviderClient client = resolver.acquireContentProviderClient(uri);
            if (client == null) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            try {
                Object mContentProvider = ReflectUtil.getFieldObject(client, ContentProviderClient.class, "mContentProvider");
                if (mContentProvider != null) {
                    //public Bundle call(String method, String request, Bundle args)
                    Object result = null;
                    try {
                        result = ReflectUtil.invokeMethod(mContentProvider, Class.forName("android.content.IContentProvider"), "call", new Class[]{String.class, String.class, Bundle.class}, new Object[]{method, arg, extras});
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return (Bundle) result;
                }

            } finally {
                client.release();
            }
            return null;
        }
    }
}
