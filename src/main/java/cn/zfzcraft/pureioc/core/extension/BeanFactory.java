package cn.zfzcraft.pureioc.core.extension;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

import cn.zfzcraft.pureioc.annotations.QualifierClass;
import cn.zfzcraft.pureioc.core.ApplicationContext;

/**
 * must be no args constructor
 */
public interface BeanFactory{

	Object  createBean(ApplicationContext applicationContext,AnnotatedElement beanElement);

	default Object[] resolveArgs(ApplicationContext applicationContext, Parameter[] parameters) {
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			QualifierClass qualifier = param.getAnnotation(QualifierClass.class);
			if (qualifier != null) {
				args[i] = applicationContext.getBean(qualifier.value());
			} else {
				args[i] = applicationContext.getBean(param.getType());
			}
		}
		return args;
	}
}
