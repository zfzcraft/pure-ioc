package cn.zfzcraft.pureioc.core.test;

import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;

@ConfigurationProperties(prefix = "test")
public class TestProperties {
    private String name = "default";
    private int count = 123;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}