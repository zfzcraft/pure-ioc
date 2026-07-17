package cn.zfzcraft.pureioc.core.test;

public class Repository {
    private final DataSource dataSource;

    public Repository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String query() {
        return "Query result from " + dataSource.getConfig().getHost();
    }
}