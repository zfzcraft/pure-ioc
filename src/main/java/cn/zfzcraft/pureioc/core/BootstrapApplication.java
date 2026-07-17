package cn.zfzcraft.pureioc.core;

public class BootstrapApplication {

	public static ApplicationContext run(String[] args, Class<?> mainClass) {
			LifeCycleApplicationContext context = new AnnotationConfigApplicationContext(args,mainClass);
			context.refresh();
			return context;
	}
}
