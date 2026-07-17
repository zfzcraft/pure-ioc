package cn.zfzcraft.pureioc.core.test;

import cn.zfzcraft.pureioc.annotations.Component;

@Component
public class TestInterfaceImpl implements TestInterface {
    @Override
    public String getValue() {
        return "impl";
    }
}