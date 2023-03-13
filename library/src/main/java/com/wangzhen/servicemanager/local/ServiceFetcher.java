package com.wangzhen.servicemanager.local;

/**
 * ServiceFetcher
 * Created by wangzhen on 2023/3/7/007
 */
public abstract class ServiceFetcher {
    int mServiceId;
    String mGroupId;
    private Object mCachedInstance;

    public final Object getService() {
        synchronized (ServiceFetcher.this) {
            Object service = mCachedInstance;
            if (service != null) {
                return service;
            }
            return mCachedInstance = createService(mServiceId);
        }
    }

    public abstract Object createService(int serviceId);

}
