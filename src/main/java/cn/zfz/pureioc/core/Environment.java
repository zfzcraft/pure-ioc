package cn.zfz.pureioc.core;

public interface Environment {
	
	Object getProperty(String key);

	<T> T getProperty(String prefix, Class<T> type);
}
