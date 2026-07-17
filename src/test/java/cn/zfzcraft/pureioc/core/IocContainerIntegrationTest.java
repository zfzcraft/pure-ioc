package cn.zfzcraft.pureioc.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.zfzcraft.pureioc.core.test.DataSource;
import cn.zfzcraft.pureioc.core.test.DatabaseConfig;
import cn.zfzcraft.pureioc.core.test.IntegrationApplication;
import cn.zfzcraft.pureioc.core.test.Repository;
import cn.zfzcraft.pureioc.core.test.Service;

public class IocContainerIntegrationTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void setUp() {
        context = new AnnotationConfigApplicationContext(null,IntegrationApplication.class);
    }

    @AfterEach
    public void tearDown() {
        if (context != null) {
            context.destroy();
        }
        ParallelClassLoader.shutdown();
    }

    @Test
    public void testFullDependencyChain() {
        context.refresh();
        Service service = context.getBean(Service.class);
        assertNotNull(service);
        assertNotNull(service.getDataSource());
        assertNotNull(service.getRepository());
        assertNotNull(service.getRepository().getDataSource());
        assertSame(service.getDataSource(), service.getRepository().getDataSource());
    }

    @Test
    public void testConfigurationBeanCreation() {
        context.refresh();
        DatabaseConfig config = context.getBean(DatabaseConfig.class);
        assertNotNull(config);
        assertEquals("localhost", config.getHost());
        assertEquals(3306, config.getPort());
    }

    @Test
    public void testEagerInitialization() {
        context.refresh();
        DatabaseConfig config = context.getBean(DatabaseConfig.class);
        assertNotNull(config);
        assertEquals("localhost", config.getHost());
    }

    @Test
    public void testBeanSingletonAcrossDependencies() {
        context.refresh();
        Service service = context.getBean(Service.class);
        Repository repository = context.getBean(Repository.class);
        DataSource dataSource1 = service.getDataSource();
        DataSource dataSource2 = repository.getDataSource();
        DataSource dataSource3 = context.getBean(DataSource.class);
        assertSame(dataSource1, dataSource2);
        assertSame(dataSource2, dataSource3);
    }

    @Test
    public void testApplicationContextRegistration() {
        context.refresh();
        ApplicationContext appContext = context.getBean(ApplicationContext.class);
        assertNotNull(appContext);
        assertSame(context, appContext);
    }

    @Test
    public void testDisposableBeanCallback() {
        context.refresh();
        DataSource dataSource = context.getBean(DataSource.class);
        assertTrue(dataSource.isConnected());
        context.destroy();
        assertFalse(dataSource.isConnected());
    }

    @Test
    public void testServiceMethodExecution() {
        context.refresh();
        Service service = context.getBean(Service.class);
        String result = service.process();
        assertTrue(result.contains("Service processed"));
        assertTrue(result.contains("Query result from localhost"));
    }

    @Test
    public void testMultipleBeanRetrievals() {
        context.refresh();
        Service service1 = context.getBean(Service.class);
        Service service2 = context.getBean(Service.class);
        Repository repository1 = context.getBean(Repository.class);
        Repository repository2 = context.getBean(Repository.class);
        assertSame(service1, service2);
        assertSame(repository1, repository2);
        assertSame(service1.getRepository(), repository1);
    }

    @Test
    public void testEnvironmentAccessInIntegration() {
        context.refresh();
        Environment environment = context.getEnvironment();
        assertNotNull(environment);
    }
}