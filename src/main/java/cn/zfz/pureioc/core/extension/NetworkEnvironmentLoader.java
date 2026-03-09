package cn.zfz.pureioc.core.extension;

import java.util.Map;

import cn.zfz.pureioc.core.Environment;
import cn.zfz.pureioc.core.ExtensionPoint;
/**
 * must be no args constructor
 */
public interface NetworkEnvironmentLoader extends ExtensionPoint{

	Map<String, String> load(Environment local);
}
