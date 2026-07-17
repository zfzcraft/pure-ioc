package cn.zfzcraft.pureioc.utils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;

public class NestedMapUtils {
	
	private static final JSONReader.Feature[] CONFIG_FEATURES = {
            JSONReader.Feature.SupportSmartMatch,
            JSONReader.Feature.IgnoreAutoTypeNotMatch
    };

	/**
	 * 从嵌套 Map 中按 a.b.c 取最终叶子值
	 */
	public static Object getNestedValue(Map<String, Object> rootMap, String key) {
		String[] paths = key.split("\\.");
		Object current = rootMap;

		for (String path : paths) {
			if (!(current instanceof Map)) {
				return null;
			}
			current = ((Map<?, ?>) current).get(path);
			if (current == null) {
				return null;
			}
		}
		return current;
	}

	public static <T> T loadAs(Map<String, Object> root, String prefix, Class<T> clazz) {
		Map<String, Object> subMap = getNestedMap(root, prefix);
		if (subMap == null || subMap.isEmpty()) {
			return null;
		}
		JSONObject jsonObject =JSONObject.from(subMap);
		return jsonObject.toJavaObject(clazz, CONFIG_FEATURES);
	}

	/**
	 * 按 a.b.c 从根 Map 中获取嵌套 Map
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getNestedMap(Map<String, Object> root, String path) {
		if (root == null) {
			return null;
		}
		if (path == null || path.isEmpty()) {
			return root;
		}
		String[] keys = path.split("\\.");
		Map<String, Object> current = root;

		for (int i = 0; i < keys.length - 1; i++) {
			if (current == null) {
				return null;
			}
			Object next = current.get(keys[i]);
			if (!(next instanceof Map)) {
				return null;
			}
			current = (Map<String, Object>) next;
		}

		if (current == null) {
			return null;
		}
		Object last = current.get(keys[keys.length - 1]);
		if (last instanceof Map) {
			return (Map<String, Object>) last;
		}
		return null;
	}

	private static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
		try {
			T instance = clazz.getDeclaredConstructor().newInstance();
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				Object rawValue = map.get(field.getName());
				if (rawValue == null) {
					continue;
				}
				Object value = convertValue(rawValue, field.getType());
				field.set(instance, value);
			}
			return instance;
		} catch (Exception e) {
			throw new RuntimeException("Failed to bind map to " + clazz.getName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Object convertValue(Object value, Class<?> targetType) {
		if (value == null) {
			return null;
		}
		if (targetType.isInstance(value)) {
			return value;
		}
		if (value instanceof Map && !isSimpleType(targetType)) {
			return mapToObject((Map<String, Object>) value, targetType);
		}
		String str = value.toString();
		if (targetType == String.class) return str;
		if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(str);
		if (targetType == long.class || targetType == Long.class) return Long.parseLong(str);
		if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(str);
		if (targetType == double.class || targetType == Double.class) return Double.parseDouble(str);
		if (targetType == float.class || targetType == Float.class) return Float.parseFloat(str);
		if (targetType == short.class || targetType == Short.class) return Short.parseShort(str);
		if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(str);
		return value;
	}

	private static boolean isSimpleType(Class<?> type) {
		return type.isPrimitive()
				|| type == String.class
				|| Number.class.isAssignableFrom(type)
				|| Boolean.class == type;
	}

	/**
	 * 往嵌套 Map 里设置值，key 是 a.b.c 格式
	 *
	 * @param rootMap 根嵌套Map
	 * @param dotKey  点分隔key，如 spring.datasource.password
	 * @param value   要覆盖的值
	 */
	@SuppressWarnings("unchecked")
	public static void setValue(Map<String, Object> rootMap, String dotKey, Object value) {
		Objects.requireNonNull(rootMap, "rootMap must not be null");
		Objects.requireNonNull(dotKey, "dotKey must not be null");

		String[] paths = dotKey.split("\\.");
		Map<String, Object> current = rootMap;

		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];

			// 最后一段：直接赋值
			if (i == paths.length - 1) {
				current.put(path, value);
				return;
			}

			// 不是最后一段，要往下走
			Object nextObj = current.get(path);

			// 下一层是 Map，继续走
			if (nextObj instanceof Map<?, ?>) {
				current = (Map<String, Object>) nextObj;
			} else {
				// 下一层不存在 / 不是Map → 新建一层覆盖
				Map<String, Object> newMap = new java.util.HashMap<>();
				current.put(path, newMap);
				current = newMap;
			}
		}
	}

}
