package com.wangzhen.servicemanager.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ReflectUtil
 * Created by wangzhen on 2023/3/7/007
 */
public class ReflectUtil {

    public static Object invokeMethod(Object target, Class<?> clazz, String methodName, Class<?>[] paramTypes, Object[] paramValues) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method.invoke(target, paramValues);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getFieldObject(Object target, Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(target);
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;

    }

}