package cn.zfzcraft.pureioc.core.test;

import cn.zfzcraft.pureioc.annotations.Component;

@Component
public class TestComponent {
    private final TestDependency dependency;

    public TestComponent(TestDependency dependency) {
        this.dependency = dependency;
    }

    public TestDependency getDependency() {
        return dependency;
    }
}