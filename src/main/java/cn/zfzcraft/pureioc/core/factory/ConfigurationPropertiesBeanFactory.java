package cn.zfzcraft.pureioc.core.factory;

import java.lang.reflect.AnnotatedElement;
import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.Environment;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;

public class ConfigurationPropertiesBeanFactory implements BeanFactory {

	@Override
	public Object createBean(ApplicationContext applicationContext, AnnotatedElement beanElement) {
		Class<?> beanClass = (Class<?>) beanElement;
		Environment environment = applicationContext.getEnvironment();
		ConfigurationProperties configurationProperties = beanClass.getAnnotation(ConfigurationProperties.class);
		String prefix = configurationProperties.prefix();
		Object beanObject = environment.getProperty(prefix, beanClass);
		return beanObject;
		
	}

}
