package cn.zfzcraft.pureioc.core.test;

public class DatabaseConfig {
    private final String host;
    private final int port;

    public DatabaseConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}