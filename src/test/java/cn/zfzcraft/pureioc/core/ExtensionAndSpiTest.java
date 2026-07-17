package cn.zfzcraft.pureioc.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.zfzcraft.pureioc.core.test.TestApplication;
import cn.zfzcraft.pureioc.core.test.TestComponent;
import cn.zfzcraft.pureioc.core.testext.TestBeanPostProcessor;
import cn.zfzcraft.pureioc.core.testext.TestEnvironmentLoader;
import cn.zfzcraft.pureioc.core.testext.TestEnvironmentPostProcessor;

public class ExtensionAndSpiTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void setUp() {
        TestBeanPostProcessor.reset();
        TestEnvironmentPostProcessor.reset();
        TestEnvironmentLoader.reset();
        context = new AnnotationConfigApplicationContext(null,TestApplication.class);
    }

    @AfterEach
    public void tearDown() {
        if (context != null) {
            context.destroy();
        }
        ParallelClassLoader.shutdown();
    }

    @Test
    public void testSpiPluginLoaded() {
        context.refresh();
        assertTrue(TestEnvironmentLoader.loaded, "TestPlugin should have registered TestEnvironmentLoader via SPI");
    }

    @Test
    public void testEnvironmentLoaderExecuted() {
        context.refresh();
        assertTrue(TestEnvironmentLoader.loaded, "EnvironmentLoader should have been executed during refresh");
        Environment env = context.getEnvironment();
        assertEquals("loaded", env.getProperty("test.loader"));
    }

    @Test
    public void testEnvironmentPostProcessorExecuted() {
        context.refresh();
        assertTrue(TestEnvironmentPostProcessor.processed, "EnvironmentPostProcessor should have been executed during refresh");
        Environment env = context.getEnvironment();
        assertEquals("true", env.getProperty("test.ext.processed"));
    }

    @Test
    public void testBeanPostProcessorExecuted() {
        context.refresh();
        TestComponent component = context.getBean(TestComponent.class);
        assertNotNull(component);
        assertTrue(TestBeanPostProcessor.processed, "BeanPostProcessor should have processed TestComponent");
        assertNotNull(TestBeanPostProcessor.processedClass);
        assertTrue(TestBeanPostProcessor.processedClass.getName().contains("TestComponent"));
    }

    @Test
    public void testExtensionOrder() {
        context.refresh();
        Environment env = context.getEnvironment();
        assertTrue(TestEnvironmentLoader.loaded, "Loader should run before PostProcessor");
        assertTrue(TestEnvironmentPostProcessor.processed, "PostProcessor should run after Loader");
        assertEquals("loaded", env.getProperty("test.loader"));
        assertEquals("true", env.getProperty("test.ext.processed"));
    }
}