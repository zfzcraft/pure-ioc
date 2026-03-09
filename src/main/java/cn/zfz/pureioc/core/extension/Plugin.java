package cn.zfz.pureioc.core.extension;
import java.util.List;

import cn.zfz.pureioc.core.ExtensionPoint;

/**
 * must be no args constructor
 */
public interface Plugin  extends ExtensionPoint{
	
    List<Class<?>> registerBeanClasses();
    
}