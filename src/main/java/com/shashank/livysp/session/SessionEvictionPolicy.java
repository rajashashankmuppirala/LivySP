package com.shashank.livysp.session;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.EvictionConfig;
import org.apache.commons.pool2.impl.EvictionPolicy;

public class SessionEvictionPolicy<T> implements EvictionPolicy<T> {

    @Override
    public boolean evict(final EvictionConfig evictionConfig, final PooledObject<T> pooledObject,
                         final int idleCount) {
        long elapsed = System.currentTimeMillis() - pooledObject.getCreateTime();
        if (evictionConfig.getIdleEvictTime() < elapsed) {
            return true;
        }
        return false;
    }
}
