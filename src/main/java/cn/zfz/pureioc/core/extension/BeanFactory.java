package cn.zfz.pureioc.core.extension;

import cn.zfz.pureioc.core.ApplicationContext;
import cn.zfz.pureioc.core.ExtensionPoint;

/**
 * must be no args constructor
 */
public interface BeanFactory  extends ExtensionPoint{

	Object  createBean(ApplicationContext applicationContext,Class<?> beanClass);
}
