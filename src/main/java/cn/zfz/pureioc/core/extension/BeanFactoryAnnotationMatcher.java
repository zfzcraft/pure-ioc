package cn.zfz.pureioc.core.extension;

import java.lang.annotation.Annotation;

import cn.zfz.pureioc.core.ExtensionPoint;

/**
 * must be no args constructor
 */
public interface BeanFactoryAnnotationMatcher extends ExtensionPoint{

	/**
	 * for interface class ,must return a BeanFactoryClass
	 * @return
	 */
	default Class<? extends BeanFactory> getBeanFactoryClass() {
		return null;
	}

	Class<? extends Annotation> getBeanAnnotationClass();
}
