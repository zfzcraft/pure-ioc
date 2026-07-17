package cn.zfzcraft.pureioc.core.testext;

import java.util.HashMap;
import java.util.Map;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.Environment;
import cn.zfzcraft.pureioc.core.extension.EnvironmentLoader;

@Extension
public class TestEnvironmentLoader implements EnvironmentLoader {

    public static boolean loaded = false;

    @Override
    public Map<String, Object> load(Environment local) {
        loaded = true;
        Map<String, Object> map = new HashMap<>();
        map.put("test", new HashMap<String, Object>() {{ put("loader", "loaded"); }});
        return map;
    }

    public static void reset() {
        loaded = false;
    }
}