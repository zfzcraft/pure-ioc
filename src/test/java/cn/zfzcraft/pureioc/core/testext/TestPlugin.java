package cn.zfzcraft.pureioc.core.testext;

import java.util.Set;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.spi.Plugin;

@Extension
public class TestPlugin implements Plugin {

    @Override
    public void registerBeanClasses(Set<Class<?>> pluginClasses) {
        pluginClasses.add(TestBeanPostProcessor.class);
        pluginClasses.add(TestEnvironmentPostProcessor.class);
        pluginClasses.add(TestEnvironmentLoader.class);
    }
}