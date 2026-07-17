package cn.zfzcraft.pureioc.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.zfzcraft.pureioc.core.test.DisposableTestBean;
import cn.zfzcraft.pureioc.core.test.TestApplication;
import cn.zfzcraft.pureioc.core.test.TestBean;
import cn.zfzcraft.pureioc.core.test.TestComponent;
import cn.zfzcraft.pureioc.core.test.TestInterface;
import cn.zfzcraft.pureioc.core.test.TestProperties;

public class AnnotationConfigApplicationContextTest {

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	public void setUp() {
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
	public void testComponentBeanCreation() {
		context.refresh();
		TestComponent component = context.getBean(TestComponent.class);
		assertNotNull(component);
		assertNotNull(component.getDependency());
	}

	@Test
	public void testConfigurationBeanCreation() {
		context.refresh();
		TestBean bean = context.getBean(TestBean.class);
		assertNotNull(bean);
		assertEquals("test", bean.getName());
	}

	@Test
	public void testInterfaceResolution() {
		context.refresh();
		TestInterface iface = context.getBean(TestInterface.class);
		assertNotNull(iface);
		assertEquals("impl", iface.getValue());
	}

	@Test
	public void testConfigurationProperties() {
		context.refresh();
		TestProperties properties = context.getBean(TestProperties.class);
		assertNotNull(properties);
		assertEquals("default", properties.getName());
		assertEquals(123, properties.getCount());
	}

	@Test
	public void testBeanSingleton() {
		context.refresh();
		TestComponent bean1 = context.getBean(TestComponent.class);
		TestComponent bean2 = context.getBean(TestComponent.class);
		assertSame(bean1, bean2);
	}

	@Test
	public void testDestroy() {
		context.refresh();
		DisposableTestBean bean = context.getBean(DisposableTestBean.class);
		assertNotNull(bean);
		assertFalse(bean.isDestroyed());
		context.destroy();
		assertTrue(bean.isDestroyed());
	}

	@Test
	public void testBeanNotExistException() {
		context.refresh();
		assertThrows(cn.zfzcraft.pureioc.core.exception.BeanNotExistException.class, () -> {
			context.getBean(NonExistentBean.class);
		});
	}

	public static class NonExistentBean {
	}
}