package cn.zfzcraft.pureioc.core.spi;
import java.util.List;

import cn.zfzcraft.pureioc.core.ExtensionPoint;

/**
 * must be no args constructor
 */
public interface Plugin  extends ExtensionPoint{
	
    List<Class<?>> registerBeanClasses();
    
}