package cn.zfzcraft.pureioc.core.testext;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.extension.BeanPostProcessor;

@Extension
public class TestBeanPostProcessor implements BeanPostProcessor {

    public static boolean processed = false;
    public static Class<?> processedClass = null;

    @Override
    public boolean matches(Class<?> beanClass) {
        return beanClass.getName().contains("TestComponent");
    }

    @Override
    public Object process(ApplicationContext applicationContext, Class<?> beanClass, Object bean) {
        processed = true;
        processedClass = beanClass;
        return bean;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static void reset() {
        processed = false;
        processedClass = null;
    }
}