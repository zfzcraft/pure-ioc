package cn.zfzcraft.pureioc.core.test;

import cn.zfzcraft.pureioc.annotations.Component;
import cn.zfzcraft.pureioc.core.DisposableBean;

@Component
public class DisposableTestBean implements DisposableBean {
    private boolean destroyed = false;

    @Override
    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}