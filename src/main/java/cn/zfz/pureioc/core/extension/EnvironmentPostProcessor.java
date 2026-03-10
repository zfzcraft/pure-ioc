package cn.zfz.pureioc.core.extension;

import cn.zfz.pureioc.core.Environment;
import cn.zfz.pureioc.core.ExtensionPoint;
/**
 * must be no args constructor
 */
public interface EnvironmentPostProcessor extends ExtensionPoint {
	
	void postProcess(Environment environment);

}
