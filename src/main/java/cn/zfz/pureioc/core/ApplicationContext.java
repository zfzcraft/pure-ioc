package cn.zfz.pureioc.core;

import java.util.List;
import java.util.Set;


public interface ApplicationContext{

	Environment getEnvironment();
	
	<T> T getBean(Class<T> clazz);

	<T> List<T> getBeansOfType(Class<T> interfaceClass);
	
	Set<Class<?>> getBeanClasses();

}
