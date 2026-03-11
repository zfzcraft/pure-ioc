package cn.zfz.pureioc.core;

import cn.zfz.pureioc.core.compoments.FrameworkCompoment;

public interface Environment extends FrameworkCompoment{
	
	Object getProperty(String key);

	<T> T getProperty(String prefix, Class<T> type);
}
