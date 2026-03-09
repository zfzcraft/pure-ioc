package cn.zfz.pureioc.core;

import cn.zfz.pureioc.annotations.Bean;
import cn.zfz.pureioc.annotations.Component;
import cn.zfz.pureioc.annotations.ConditionalOnClass;
import cn.zfz.pureioc.annotations.ConditionalOnMissingBean;
import cn.zfz.pureioc.annotations.ConditionalOnPropertity;
import cn.zfz.pureioc.annotations.Configuration;
import cn.zfz.pureioc.annotations.ConfigurationProperties;
import cn.zfz.pureioc.annotations.Eager;
import cn.zfz.pureioc.annotations.Imports;
import cn.zfz.pureioc.core.extension.BeanFactory;
import cn.zfz.pureioc.core.extension.BeanPostProcessor;
import cn.zfz.pureioc.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfz.pureioc.core.extension.NetworkEnvironmentLoader;
import cn.zfz.pureioc.core.extension.Plugin;
import cn.zfz.pureioc.utils.AnnotationUtils;
import cn.zfz.pureioc.utils.NestedMapUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AnnotationConfigApplicationContext implements LifeCycleApplicationContext {

	private static final String BASE_CONFIG = "app.yml";

	private AtomicBoolean refresh = new AtomicBoolean(false);

	private AtomicBoolean preheatComplete = new AtomicBoolean(false);

	private Class<?> maincClass;

	private String[] args;

	private Map<String, Object> env = new HashMap<>();

	private List<Plugin> plugins = new ArrayList<>();

	private List<Class<?>> beanPostProcessorClasses = new ArrayList<>();

	List<Class<?>> applicationClasses = new ArrayList<>();

	List<Class<?>> pluginClasses = new ArrayList<>();

	List<Class<? extends Annotation>> beanAnnotationClasses = new ArrayList<>();

	private List<Class<?>> beanFactoryClasses = new ArrayList<>();

	private List<BeanFactoryAnnotationMatcher> beanFactoryAnnotationMatchers = new ArrayList<>();

	// 按 order 升序：越小越先
	private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

	private Map<Class<?>, BeanDefinition> beanDefinitionMap = new HashMap<>();

	private final Map<Class<?>, Object> singletonPool = new ConcurrentHashMap<>();

	private Map<Class<?>, Object> configurationMap = new ConcurrentHashMap<>();

	private Map<Class<? extends BeanFactory>, BeanFactory> beanFactoryMap = new ConcurrentHashMap<>();
	
	private Environment environment = new LocalEnvironment(env);

	@Override
	public void setMaincClass(Class<?> maincClass) {
		this.maincClass = maincClass;
	}

	@Override
	public void setArgs(String[] args) {
		this.args = args;
	}

	@Override
	public void refresh() {
		if (refresh.compareAndSet(false, true)) {
			System.out.println("开始启动容器............");
			loadEnvironment();
			loadPlugin();
			loadPluginClasses();
			loadNetworkEnvironment();
			collectFactoryBeanMatchers();
			collectBeanFactoryAndAnnotation();
			scanPackageClasses();
			collectBeanPostProcessors();
			registerBeanDefinitions();
			registerApplicationContext();
			instantiateBeanPostProcessors();
			instantiateEagerBeans();
			System.out.println("启动容器成功............");
			asyncInstantiateLazyBeansAndClearResources();
			registerShutdownHook();
		}
	}

	private void loadNetworkEnvironment() {
		Environment environment = new LocalEnvironment(env);
		List<Class<?>> list = pluginClasses.stream().filter(ele->NetworkEnvironmentLoader.class.isAssignableFrom(ele)).collect(Collectors.toList());
		for (Class<?> networkEnvironmentLoaderClass : list) {
			try {
				NetworkEnvironmentLoader loader = (NetworkEnvironmentLoader) networkEnvironmentLoaderClass.getConstructor().newInstance();
				Map<String, String> networkMap = loader.load(environment);
				ConfigLoader.applyFlatArgsToNestedMap( env,networkMap);
			} catch (Exception e) {
				throw IocException.of(e);
			}
			
		}

	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			destory();
		}));
	}

	private void cleanResource() {
		maincClass = null;
		args = null;
		env.clear();
		env = null;
		configurationMap.clear();
		configurationMap = null;
		beanFactoryMap.clear();
		plugins.clear();
		beanPostProcessorClasses.clear();
		pluginClasses.clear();
		beanAnnotationClasses.clear();
		beanFactoryClasses.clear();
		beanFactoryAnnotationMatchers.clear();
		beanDefinitionMap.clear();
		beanPostProcessors.clear();
		applicationClasses.clear();

		beanFactoryMap = null;
		plugins = null;
		beanPostProcessorClasses = null;
		pluginClasses = null;
		beanAnnotationClasses = null;
		beanFactoryClasses = null;
		beanFactoryAnnotationMatchers = null;
		beanDefinitionMap = null;
		beanPostProcessors = null;
		applicationClasses = null;

	}

	private void registerApplicationContext() {
		beanDefinitionMap.putIfAbsent(ApplicationContext.class, new ClassBeanDefinition(this.getClass(), false));
		singletonPool.putIfAbsent(ApplicationContext.class, this);
	}

	private void asyncInstantiateLazyBeansAndClearResources() {
		new Thread(() -> {
			for (Entry<Class<?>, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
				Class<?> key = entry.getKey();
				BeanDefinition value = entry.getValue();
				if (!value.isEager()) {
					getBean(key);
				}
			}
			System.out.println("异步预热成功............");
			preheatComplete.compareAndSet(false, true);
			cleanResource();
			System.out.println("清理资源成功............");
		}).start();
	}

	private void collectFactoryBeanMatchers() {
		for (Class<?> clazz : pluginClasses) {
			if (BeanFactoryAnnotationMatcher.class.isAssignableFrom(clazz)) {
				try {
					beanFactoryAnnotationMatchers
							.add((BeanFactoryAnnotationMatcher) clazz.getConstructor().newInstance());
				} catch (Exception e) {
					throw IocException.of(e);
				}
			}
		}
	}

	private void collectBeanPostProcessors() {
		for (Class<?> clazz : applicationClasses) {
			if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
				beanPostProcessorClasses.add(clazz);
			}
		}
		for (Class<?> clazz : pluginClasses) {
			if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
				beanPostProcessorClasses.add(clazz);
			}
		}
	}

	private void collectBeanFactoryAndAnnotation() {
		for (BeanFactoryAnnotationMatcher factoryBeanMatcher : beanFactoryAnnotationMatchers) {
			beanAnnotationClasses.add(factoryBeanMatcher.getBeanAnnotationClass());
			beanFactoryClasses.add(factoryBeanMatcher.getBeanAnnotationClass());
		}
	}

	private void loadPluginClasses() {
		for (Plugin plugin : plugins) {
			List<Class<?>> loadClasses = plugin.registerBeanClasses();
			for (Class<?> loadClass : loadClasses) {
				pluginClasses.add(loadClass);
				if (loadClass.isAnnotationPresent(Imports.class)
						&& loadClass.isAnnotationPresent(Configuration.class)) {
					Imports imports = loadClass.getAnnotation(Imports.class);
					for (Class<?> clazz : imports.value()) {
						pluginClasses.add(clazz);
					}
				}
			}
		}
	}

	// ==========================
	// 加载配置
	// ==========================
	private void loadEnvironment() {
		env = ConfigLoader.loadConfig(BASE_CONFIG, args);
	}

	private void scanPackageClasses() {
		String pkg = maincClass.getPackageName();
		String path = pkg.replace('.', '/');
		try {
			Enumeration<URL> urls = getClass().getClassLoader().getResources(path);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				File dir = new File(url.getFile());
				scanDir(dir, pkg);
			}
		} catch (Exception e) {
			throw IocException.of(e);
		}
	}

	private void scanDir(java.io.File dir, String pkg) {
		if (!dir.isDirectory()) {
			return;
		}
		java.io.File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File classFile : files) {
			if (classFile.isDirectory()) {
				scanDir(classFile, pkg + "." + classFile.getName());
			} else if (classFile.getName().endsWith(".class")) {
				byte[] classBytes = readClassBytes(classFile);
				String className = pkg + "." + classFile.getName().replace(".class", "");

				if (isAnnotationClass(classBytes)) {
					try {
						Class<?> clazz = Class.forName(className);
						applicationClasses.add(clazz);
					} catch (Exception e) {
						throw IocException.of(e);
					}
				}
			}
		}
	}

	private byte[] readClassBytes(File classFile) {
		try (InputStream in = new FileInputStream(classFile);
	             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
	            byte[] buf = new byte[8192];
	            int len;
	            while ((len = in.read(buf)) != -1) {
	                out.write(buf, 0, len);
	            }
	            return out.toByteArray();
	        } catch (IOException e) {
	            throw IocException.of(e);
	        }
	}

	private boolean isAnnotationClass(byte[] classBytes) {
		for (Class<? extends Annotation> anno : beanAnnotationClasses) {
			if (AnnotationUtils.hasAnnotation(classBytes, anno)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAnnotationClass(Class<?> clazz) {
		for (Class<? extends Annotation> anno : beanAnnotationClasses) {
			if (clazz.isAnnotationPresent(anno)) {
				return true;
			}
		}
		return false;
	}

	private void loadPlugin() {
		ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
		for (Plugin plugin : loader) {
			plugins.add(plugin);
		}
	}

	private void registerBeanDefinitions() {
		for (Class<?> clazz : applicationClasses) {
			registerBeanDefinition(clazz);
		}
		for (Class<?> clazz : pluginClasses) {
			registerBeanDefinition(clazz);
		}
	}

	private void registerBeanDefinition(Class<?> clazz) {
		if (clazz.isInterface()) {
			for (BeanFactoryAnnotationMatcher factoryBeanMatcher : beanFactoryAnnotationMatchers) {
				if (clazz.isAnnotationPresent(factoryBeanMatcher.getBeanAnnotationClass())) {
					boolean eager = getEager(clazz);
					beanDefinitionMap.putIfAbsent(clazz,
							new InterfaceBeanDefinition(clazz, factoryBeanMatcher.getBeanFactoryClass(), eager));
				}
			}
		}
		if (clazz.isAnnotationPresent(Component.class) && isNormalClass(clazz)) {
			boolean eager = getEager(clazz);
			beanDefinitionMap.putIfAbsent(clazz, new ClassBeanDefinition(clazz, eager));
		}
		if (clazz.isAnnotationPresent(ConfigurationProperties.class) && isNormalClass(clazz)) {
			beanDefinitionMap.putIfAbsent(clazz, new PropertiesBeanDefinition(clazz, false));
		}
		if (clazz.isAnnotationPresent(Configuration.class) && isNormalClass(clazz)) {
			if (hasCondition(clazz)) {
				if (isConditionTrue(clazz)) {
					registerConfigurationBeanDefinition(clazz);
				}
			} else {
				registerConfigurationBeanDefinition(clazz);
			}
		}
		if (isAnnotationClass(clazz)) {
			for (BeanFactoryAnnotationMatcher factoryBeanMatcher : beanFactoryAnnotationMatchers) {
				if (clazz.isAnnotationPresent(factoryBeanMatcher.getBeanAnnotationClass())) {
					boolean eager = getEager(clazz);
					beanDefinitionMap.putIfAbsent(clazz,
							new ClassBeanDefinition(clazz, eager));
				}
			}
		}
	}

	private void registerConfigurationBeanDefinition(Class<?> clazz) {
		boolean eager = getEager(clazz);
		beanDefinitionMap.putIfAbsent(clazz, new ClassBeanDefinition(clazz, eager));
		for (Method beanMethod : clazz.getDeclaredMethods()) {
			if (hasCondition(beanMethod)) {
				if (isConditionTrue(beanMethod)) {
					registerBeanDefinition(clazz, beanMethod);
				}
			} else {
				registerBeanDefinition(clazz, beanMethod);
			}
		}
	}

	private void registerBeanDefinition(Class<?> clazz, Method beanMethod) {
		if (beanMethod.isAnnotationPresent(Bean.class)) {
			boolean eagerMethod = getEager(beanMethod);
			Class<?> returnType = beanMethod.getReturnType();
			beanDefinitionMap.putIfAbsent(returnType, new MethodBeanDefinition(clazz, beanMethod, eagerMethod));
		}
	}

	private boolean getEager(Method beanMethod) {
		if (beanMethod.isAnnotationPresent(Eager.class)) {
			return true;
		}
		return false;
	}

	private boolean getEager(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Eager.class)) {
			return true;
		}
		return false;
	}

	private void instantiateEagerBeans() {
		for (Entry<Class<?>, BeanDefinition> entry : beanDefinitionMap.entrySet()) {

			Class<?> key = entry.getKey();
			BeanDefinition value = entry.getValue();
			if (value.isEager()) {
				createBean(key);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clazz) {
		Class<T> targetClass = clazz;
		if (!isUsableBeanClass(clazz)) {
			throw IocException.of("Class must be interface or instantiable class");
		}
		if (preheatComplete.get() == true) {
			if (clazz.isInterface()) {
				List<T> beans = getBeansOfType(clazz);
				if (beans.isEmpty()) {
					throw IocException.of("No such BeanDefinition");
				} else if (beans.size() > 1) {
					throw IocException.of("Too Many BeanDefinition");
				} else {
					return beans.get(0);
				}
			} else {
				T bean = (T) singletonPool.computeIfAbsent(clazz, func -> {
					return createBean(clazz);
				});
				if (Objects.isNull(bean)) {
					throw IocException.of("No such BeanDefinition");
				}
				return bean;
			}
		} else {
			if (clazz.isInterface()) {
				List<Class<T>> keys = getImplementations(clazz);
				if (keys.isEmpty()) {
					throw IocException.of("No such BeanDefinition:" + clazz.getName());
				} else if (keys.size() > 1) {
					throw IocException.of("Too Many BeanDefinition");
				} else {
					targetClass = (Class<T>) keys.get(0);
				}
			}
			final Class<T> beanClass = targetClass;
			T bean = (T) singletonPool.computeIfAbsent(beanClass, func -> {
				return createBean(beanClass);
			});

			if (Objects.isNull(bean)) {
				throw IocException.of("No such BeanDefinition");
			}
			return bean;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> List<Class<T>> getImplementations(Class<T> clazz) {
		List<Class<T>> keys = new ArrayList<>();
		for (Class<?> key : beanDefinitionMap.keySet()) {
			if (clazz.isAssignableFrom(key)) {
				keys.add((Class<T>) key);
			}
		}
		return keys;
	}

	private boolean isUsableBeanClass(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		if (clazz.isInterface()) {
			return true;
		}
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isEnum()
				&& !clazz.isAnnotation() && !clazz.isArray() && !clazz.isPrimitive() && clazz != void.class
				&& clazz != Void.class;
	}

	private Object createBean(Class<?> clazz) {
		try {
			BeanDefinition bd = beanDefinitionMap.get(clazz);
			Object instance = null;
			if (bd.beanDefinitionType() == BeanDefinitionType.PROPERTIES) {
				PropertiesBeanDefinition propertiesBeanDefinition = bd.wrap();
				Class<?> beanClass = propertiesBeanDefinition.getBeanClass();
				instance = createPropertiesBean(beanClass);
				return instance;
			}
			if (bd.beanDefinitionType() == BeanDefinitionType.CLASS) {
				ClassBeanDefinition classBeanDefinition = bd.wrap();
				Class<?> beanClass = classBeanDefinition.getBeanClass();
				instance = createClassBean(beanClass);
			}
			if (bd.beanDefinitionType() == BeanDefinitionType.METHOD) {
				MethodBeanDefinition methodBeanDefinition = bd.wrap();
				Class<?> configurationClass = methodBeanDefinition.getConfigurationClass();
				Method beanMethod = methodBeanDefinition.getBeanMethod();
				instance = createMethodBean(configurationClass, beanMethod);
			}
			if (bd.beanDefinitionType() == BeanDefinitionType.INTERFACE) {
				InterfaceBeanDefinition interfaceBeanDefinition = bd.wrap();
				Class<?> beanClass = interfaceBeanDefinition.getBeanClass();
				Class<? extends BeanFactory> beanFactoryClass = interfaceBeanDefinition.getBeanFactory();
				instance = createInterfaceBean(beanClass, beanFactoryClass);
			}
			if (instance instanceof InitializingBean) {
				((InitializingBean) instance).afterPropertiesSet();
			}
			for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
				if (beanPostProcessor.matche(clazz)) {
					instance = beanPostProcessor.postProcess(this, instance);
				}
			}
			ProxyContext.bind(clazz, instance);
			return instance;
		} catch (Exception e) {
			throw IocException.of(e);
		}
	}

	private Object createInterfaceBean(Class<?> beanClass, Class<? extends BeanFactory> beanFactoryClass) {
		Object instance;
		BeanFactory beanFactoryInstance = getBeanFactory(beanFactoryClass);
		instance = beanFactoryInstance.createBean(this,beanClass);
		return instance;
	}

	private Object createMethodBean(Class<?> configurationClass, Method beanMethod)
			throws IllegalAccessException, InvocationTargetException {
		Object instance;
		Object configurationInstance = getConfiguration(configurationClass);
		Class<?>[] types = beanMethod.getParameterTypes();
		Object[] methodArgs = resolveArgs(types);
		instance = beanMethod.invoke(configurationInstance, methodArgs);
		return instance;
	}

	private Object createClassBean(Class<?> beanClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {			Constructor<?> ctor = beanClass.getConstructors()[0];
			Object[] args = resolveArgs(ctor.getParameterTypes());
			Object instance = ctor.newInstance(args);
			return instance;
		
	}

	private Object createPropertiesBean(Class<?> beanClass) {
		ConfigurationProperties configurationProperties = beanClass.getAnnotation(ConfigurationProperties.class);
		String prefix = configurationProperties.prefix();
		return environment.getProperty(prefix, beanClass);
	}

	private void instantiateBeanPostProcessors() {
		for (Class<?> bbpClass : beanPostProcessorClasses) {
			Constructor<?> ctor = bbpClass.getConstructors()[0];
			try {
				BeanPostProcessor beanPostProcessor = (BeanPostProcessor) ctor.newInstance();
				beanPostProcessors.add(beanPostProcessor);
			} catch (Exception e) {
				throw IocException.of(e);
			}
		}
		beanPostProcessors.sort(Comparator.comparingInt(BeanPostProcessor::getOrder));
	}

	private BeanFactory getBeanFactory(Class<? extends BeanFactory> beanFactoryClass) {
		return beanFactoryMap.computeIfAbsent(beanFactoryClass, func -> {
			try {
				return beanFactoryClass.getConstructor().newInstance();
			} catch (Exception e) {
				throw IocException.of(e);
			}
		});
	}

	private Object getConfiguration(Class<?> configurationClass) {
		return configurationMap.computeIfAbsent(configurationClass, func -> {
			try {
				Constructor<?> ctor = configurationClass.getConstructors()[0];
				Object[] args = resolveArgs(ctor.getParameterTypes());
				return ctor.newInstance(args);
			} catch (Exception e) {
				throw IocException.of(e);
			}
		});
	}

	private Object[] resolveArgs(Class<?>[] types) {
		Object[] args = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			args[i] = getBean(types[i]);
		}
		return args;
	}

	private boolean isNormalClass(Class<?> clazz) {
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isEnum()
				&& !clazz.isAnnotation();
	}

	@Override
	public void destory() {
		for (Entry<Class<?>, Object> entry : singletonPool.entrySet()) {
			Object bean = entry.getValue();
			if (bean instanceof DisposableBean disposableBean) {
				disposableBean.destroy();
			}
		}
		singletonPool.clear();
	}

	/**
	 * 判断类是否存在（标准 JVM 方式：仅检查 .class 资源，不加载类）
	 * 
	 * @param className 全类名，如 com.zaxxer.hikari.HikariDataSource
	 * @return 存在返回 true，不存在 false
	 */
	private boolean isClassPresent(String className) {
		if (className == null || className.isBlank()) {
			return false;
		}
		String resourceName = className.replace('.', '/') + ".class";
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = getClass().getClassLoader();
		}
		return classLoader.getResource(resourceName) != null;
	}

	private boolean matchesProperty(Map<String, Object> config, String propertyKey, Object havingValue) {
		if (config == null || config.isEmpty()) {
			return false;
		}
		Object actualValue = NestedMapUtils.getNestedValue(config, propertyKey);
		if (actualValue == null) {
			return false;
		}
		return isValueMatch(actualValue, havingValue);
	}

	private boolean isValueMatch(Object actual, Object expected) {
		if (Objects.equals(actual, expected)) {
			return true;
		}
		// 布尔宽松匹配：true/"true"/"TRUE"/"True" 都算 true
		if (expected instanceof Boolean) {
			String actualStr = actual.toString().trim().toLowerCase();
			return Boolean.parseBoolean(actualStr) == (Boolean) expected;
		}
		// 字符串忽略大小写匹配
		if (expected instanceof String && actual instanceof String) {
			return ((String) expected).equalsIgnoreCase((String) actual);
		}
		return false;
	}

	@Override
	public <T> List<T> getBeansOfType(Class<T> interfaceClass) {
		if (!interfaceClass.isInterface()) {
			throw IocException.of("Class must be interface");
		}
		List<Class<T>> keys = getImplementations(interfaceClass);
		List<T> list = new ArrayList<>();
		for (Class<T> key : keys) {
			list.add(getBean(key));
		}
		return list;
	}

	private boolean hasCondition(AnnotatedElement annotatedElement) {
		if (annotatedElement.isAnnotationPresent(ConditionalOnClass.class)) {
			return true;
		}
		if (annotatedElement.isAnnotationPresent(ConditionalOnMissingBean.class)) {
			return true;
		}
		if (annotatedElement.isAnnotationPresent(ConditionalOnPropertity.class)) {
			return true;
		}
		return false;
	}

	private boolean isConditionTrue(AnnotatedElement annotatedElement) {
		if (annotatedElement.isAnnotationPresent(ConditionalOnClass.class)) {
			ConditionalOnClass conditionalOnClass = annotatedElement.getAnnotation(ConditionalOnClass.class);
			String className = conditionalOnClass.className();
			if (isClassPresent(className)) {
				return true;
			} else {
				return false;
			}
		}
		if (annotatedElement.isAnnotationPresent(ConditionalOnPropertity.class)) {
			ConditionalOnPropertity conditionalOnPropertity = annotatedElement
					.getAnnotation(ConditionalOnPropertity.class);
			String key = conditionalOnPropertity.key();
			String value = conditionalOnPropertity.value();
			if (matchesProperty(env, key, value)) {
				return true;
			} else {
				return false;
			}
		}
		if (annotatedElement.isAnnotationPresent(ConditionalOnMissingBean.class)) {
			ConditionalOnMissingBean conditionalOnMissingBean = annotatedElement
					.getAnnotation(ConditionalOnMissingBean.class);
			Class<?> key = conditionalOnMissingBean.beanClass();
			if (key.isInterface()) {
				List<Class<?>> classes = getImplementationCLasses(key);
				if (classes.isEmpty()) {
					return false;
				} else if (classes.size() == 1) {
					return true;
				} else {
					throw IocException.of("Too Many BeanDefinition");
				}
			} else {
				BeanDefinition beanDefinition = beanDefinitionMap.get(key);
				if (Objects.isNull(beanDefinition)) {
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	private List<Class<?>> getImplementationCLasses(Class<?> clazz) {
		List<Class<?>> keys = new ArrayList<>();
		for (Class<?> key : beanDefinitionMap.keySet()) {
			if (clazz.isAssignableFrom(key)) {
				keys.add(key);
			}
		}
		return keys;
	}

	@Override
	public Set<Class<?>> getBeanClasses() {
		return beanDefinitionMap.keySet();
	}

	@Override
	public Environment getEnvironment() {
		return environment;
	}

}