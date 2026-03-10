package cn.zfz.pureioc.utils;

import java.io.InputStream;

public class ResourceLoader {

	public static InputStream load(String file) {
	return	Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
	}
}
