package com.shashank.livysp.session;

import com.shashank.livysp.dto.Session;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class SessionPool extends GenericObjectPool<Session> {
    private static GenericObjectPoolConfig defaultPoolConfig = new GenericObjectPoolConfig();

    //default values
    static {
        defaultPoolConfig.setJmxEnabled(false);
        defaultPoolConfig.setMinIdle(2);
        defaultPoolConfig.setMaxIdle(2);
        defaultPoolConfig.setMaxTotal(2);
        defaultPoolConfig.setTestOnCreate(true);
        defaultPoolConfig.setTestOnBorrow(true);
        defaultPoolConfig.setTestWhileIdle(true);
        defaultPoolConfig.setFairness(true);
        defaultPoolConfig.setLifo(false);

    }

    public SessionPool(PooledObjectFactory<Session> factory) {
        super(factory, defaultPoolConfig);
    }

    public SessionPool(PooledObjectFactory<Session> factory, GenericObjectPoolConfig config) {
        super(factory, config);
    }

    public SessionPool(PooledObjectFactory<Session> factory, GenericObjectPoolConfig config, AbandonedConfig defaultAbandonedConfig) {
        super(factory, config);
    }

    @Override
    public Session borrowObject() throws Exception {
        Session session = super.borrowObject();
        return session;
    }
}
