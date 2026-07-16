package cn.zfzcraft.pureioc.core.matcher;

import java.lang.annotation.Annotation;

import cn.zfzcraft.pureioc.annotations.Component;
import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
import cn.zfzcraft.pureioc.core.factory.ComponentBeanFactory;
import cn.zfzcraft.pureioc.core.extension.BeanFactoryAnnotationMatcher;
@Extension
public class ComponentBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	@Override
	public BeanFactory getBeanFactory() {
		return new ComponentBeanFactory();
	}

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Component.class;
	}

}
