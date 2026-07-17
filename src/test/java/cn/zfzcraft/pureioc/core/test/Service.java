package cn.zfzcraft.pureioc.core.test;

public class Service {
    private final DataSource dataSource;
    private final Repository repository;

    public Service(DataSource dataSource, Repository repository) {
        this.dataSource = dataSource;
        this.repository = repository;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Repository getRepository() {
        return repository;
    }

    public String process() {
        return "Service processed: " + repository.query();
    }
}