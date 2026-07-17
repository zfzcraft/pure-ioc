package cn.zfzcraft.pureioc.core.test;

import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.Configuration;

@Configuration
public class TestConfiguration {
    @Bean
    public TestBean testBean() {
        return new TestBean("test");
    }
}