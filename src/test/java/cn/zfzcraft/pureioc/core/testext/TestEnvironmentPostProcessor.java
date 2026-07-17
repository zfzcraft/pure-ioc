package cn.zfzcraft.pureioc.core.testext;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.Environment;
import cn.zfzcraft.pureioc.core.extension.EnvironmentPostProcessor;

@Extension
public class TestEnvironmentPostProcessor implements EnvironmentPostProcessor {

    public static boolean processed = false;

    @Override
    public void process(Environment environment) {
        processed = true;
        environment.setProperty("test.ext.processed", "true");
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static void reset() {
        processed = false;
    }
}