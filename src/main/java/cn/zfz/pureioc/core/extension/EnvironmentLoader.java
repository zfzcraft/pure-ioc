package cn.zfz.pureioc.core.extension;

import java.util.Map;

import cn.zfz.pureioc.core.Environment;
import cn.zfz.pureioc.core.ExtensionPoint;
/**
 * must be no args constructor
 */
public interface EnvironmentLoader extends ExtensionPoint{

	/**
	 * 
	 * @param local
	 * @return nested map
	 */
	Map<String, Object> load(Environment local);
}
