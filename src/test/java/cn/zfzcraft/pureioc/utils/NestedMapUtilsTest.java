package cn.zfzcraft.pureioc.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class NestedMapUtilsTest {

	@Test
	public void testGetNestedValue() {
		Map<String, Object> root = new HashMap<>();
		Map<String, Object> level1 = new HashMap<>();
		Map<String, Object> level2 = new HashMap<>();
		level2.put("value", "test");
		level1.put("level2", level2);
		root.put("level1", level1);

		assertEquals("test", NestedMapUtils.getNestedValue(root, "level1.level2.value"));
		assertNull(NestedMapUtils.getNestedValue(root, "level1.nonexistent"));
		assertNull(NestedMapUtils.getNestedValue(root, "nonexistent.level2.value"));
		assertNull(NestedMapUtils.getNestedValue(null, "level1.level2.value"));
	}

	@Test
	public void testSetValue() {
		Map<String, Object> root = new HashMap<>();

		NestedMapUtils.setValue(root, "a.b.c", "test");
		assertEquals("test", NestedMapUtils.getNestedValue(root, "a.b.c"));

		NestedMapUtils.setValue(root, "x", "direct");
		assertEquals("direct", root.get("x"));
	}

	@Test
	public void testLoadAs() {
		Map<String, Object> root = new HashMap<>();
		Map<String, Object> testMap = new HashMap<>();
		testMap.put("name", "testName");
		testMap.put("count", "42");
		testMap.put("enabled", "true");
		root.put("test", testMap);

		TestConfig config = NestedMapUtils.loadAs(root, "test", TestConfig.class);
		assertNotNull(config);
		assertEquals("testName", config.getName());
		assertEquals(42, config.getCount());
		assertTrue(config.isEnabled());

		assertNull(NestedMapUtils.loadAs(root, "nonexistent", TestConfig.class));
		assertNull(NestedMapUtils.loadAs(null, "test", TestConfig.class));
	}

	@Test
	public void testLoadAsWithNestedObject() {
		Map<String, Object> root = new HashMap<>();
		Map<String, Object> outer = new HashMap<>();
		Map<String, Object> inner = new HashMap<>();
		inner.put("value", "innerValue");
		outer.put("inner", inner);
		outer.put("name", "outerName");
		root.put("outer", outer);

		OuterConfig config = NestedMapUtils.loadAs(root, "outer", OuterConfig.class);
		assertNotNull(config);
		assertEquals("outerName", config.getName());
		assertNotNull(config.getInner());
		assertEquals("innerValue", config.getInner().getValue());
	}

	public static class TestConfig {
		private String name;
		private int count;
		private boolean enabled;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class OuterConfig {
		private String name;
		private InnerConfig inner;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public InnerConfig getInner() {
			return inner;
		}

		public void setInner(InnerConfig inner) {
			this.inner = inner;
		}
	}

	public static class InnerConfig {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}