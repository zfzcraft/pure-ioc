package cn.zfzcraft.pureioc.core.test;

import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.Configuration;
import cn.zfzcraft.pureioc.annotations.Eager;

@Configuration
public class IntegrationConfig {

    @Bean
    @Eager
    public DatabaseConfig databaseConfig() {
        return new DatabaseConfig("localhost", 3306);
    }

    @Bean
    public DataSource dataSource(DatabaseConfig config) {
        return new DataSource(config);
    }

    @Bean
    public Service service(DataSource dataSource, Repository repository) {
        return new Service(dataSource, repository);
    }

    @Bean
    public Repository repository(DataSource dataSource) {
        return new Repository(dataSource);
    }
}