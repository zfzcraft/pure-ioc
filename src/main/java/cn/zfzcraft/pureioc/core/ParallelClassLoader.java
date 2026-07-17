package cn.zfzcraft.pureioc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.zfzcraft.pureioc.utils.ClassLoaderUtils;

public class ParallelClassLoader {

	private static final int CPU = Runtime.getRuntime().availableProcessors();

	private static final Object lock = new Object();
	private static volatile ThreadPoolExecutor threadPool;

	private static ThreadPoolExecutor getThreadPool() {
		if (threadPool == null) {
			synchronized (lock) {
				if (threadPool == null) {
					threadPool = new ThreadPoolExecutor(0, CPU, 60L, TimeUnit.SECONDS,
							new LinkedBlockingQueue<>(256), new ThreadPoolExecutor.CallerRunsPolicy());
				}
			}
		}
		return threadPool;
	}

	public static List<Class<?>> load(List<String> classNameList) {
		if (classNameList == null || classNameList.isEmpty()) {
			return new ArrayList<>();
		}
		List<Class<?>> applicationClasses = new ArrayList<>();
		List<List<String>> splitList = splitList(classNameList, CPU);
		List<CompletableFuture<List<Class<?>>>> futures = new ArrayList<>();
		ThreadPoolExecutor pool = getThreadPool();
		for (List<String> batch : splitList) {
			CompletableFuture<List<Class<?>>> future = CompletableFuture.supplyAsync(() -> {
				List<Class<?>> partClassList = new ArrayList<>();
				for (String className : batch) {
					try {
						Class<?> clazz = Class.forName(className, false, ClassLoaderUtils.getClassLoader());
						partClassList.add(clazz);
					} catch (Exception e) {
						throw new RuntimeException("Failed to load class: " + className, e);
					}
				}
				return partClassList;
			}, pool);
			futures.add(future);
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		for (CompletableFuture<List<Class<?>>> future : futures) {
			try {
				applicationClasses.addAll(future.get());
			} catch (Exception e) {
				throw new RuntimeException("Load Class Failed", e);
			}
		}
		return applicationClasses;
	}

	public static void shutdown() {
		synchronized (lock) {
			if (threadPool != null) {
				threadPool.shutdown();
				try {
					if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
						threadPool.shutdownNow();
					}
				} catch (InterruptedException e) {
					threadPool.shutdownNow();
					Thread.currentThread().interrupt();
				}
				threadPool = null;
			}
		}
	}

	private static List<List<String>> splitList(List<String> source, int part) {
		List<List<String>> result = new ArrayList<>();
		int total = source.size();
		if (total == 0) {
			return result;
		}
		int step = (total + part - 1) / part;
		for (int i = 0; i < part; i++) {
			int start = i * step;
			int end = Math.min(start + step, total);
			if (start >= end) {
				break;
			}
			result.add(new ArrayList<>(source.subList(start, end)));
		}
		return result;
	}
}
