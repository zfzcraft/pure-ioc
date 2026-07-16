package cn.zfzcraft.pureioc.core;

public interface LifeCycleApplicationContext extends ApplicationContext {
	
	

	void refresh();

	void destroy();

	void setArgs(String[] args);

	void setMainClass(Class<?> mainClass);
}
