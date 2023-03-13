package com.wangzhen.servicemanager.compat;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.ArrayMap;

import com.wangzhen.servicemanager.util.ReflectUtil;

import java.util.Map;

/**
 * BundleCompat
 * Created by wangzhen on 2023/3/7/007
 */
public class BundleCompat {

    public static IBinder getBinder(Bundle bundle, String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return bundle.getBinder(key);
        } else {
            return (IBinder) ReflectUtil.invokeMethod(bundle, Bundle.class, "getIBinder", new Class[]{String.class}, new Object[]{key});
        }
    }

    public static void putBinder(Bundle bundle, String key, IBinder iBinder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bundle.putBinder(key, iBinder);
        } else {
            ReflectUtil.invokeMethod(bundle, Bundle.class, "putIBinder", new Class[]{String.class, IBinder.class}, new Object[]{key, iBinder});
        }
    }

    public static void putObject(Bundle bundle, String key, Object value) {
        if (Build.VERSION.SDK_INT < 19) {
            ReflectUtil.invokeMethod(bundle, Bundle.class, "unparcel", (Class<?>[]) null, (Object[]) null);
            Map<String, Object> map = (Map<String, Object>) ReflectUtil.getFieldObject(bundle, Bundle.class, "map");
            if (map != null) {
                map.put(key, value);
            }
        } else if (Build.VERSION.SDK_INT < 21) {
            ReflectUtil.invokeMethod(bundle, Bundle.class, "unparcel", (Class<?>[]) null, (Object[]) null);
            ArrayMap<String, Object> map = (ArrayMap<String, Object>) ReflectUtil.getFieldObject(bundle, Bundle.class, "mMap");
            if (map != null) {
                map.put(key, value);
            }
        } else {
            ReflectUtil.invokeMethod(bundle, android.os.BaseBundle.class, "unparcel", (Class<?>[]) null, (Object[]) null);
            ArrayMap<String, Object> map = (ArrayMap<String, Object>) ReflectUtil.getFieldObject(bundle, android.os.BaseBundle.class, "mMap");
            if (map != null) {
                map.put(key, value);
            }
        }
    }
}
