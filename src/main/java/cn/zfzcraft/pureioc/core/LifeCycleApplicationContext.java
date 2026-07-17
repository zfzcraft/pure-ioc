package cn.zfzcraft.pureioc.core;

public interface LifeCycleApplicationContext extends ApplicationContext {
	
	

	void refresh();

	void destroy();
}
