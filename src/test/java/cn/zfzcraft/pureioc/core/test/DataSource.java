package cn.zfzcraft.pureioc.core.test;

import cn.zfzcraft.pureioc.core.DisposableBean;

public class DataSource implements DisposableBean {
    private final DatabaseConfig config;
    private boolean connected = false;

    public DataSource(DatabaseConfig config) {
        this.config = config;
        this.connected = true;
    }

    public DatabaseConfig getConfig() {
        return config;
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void destroy() {
        this.connected = false;
    }
}