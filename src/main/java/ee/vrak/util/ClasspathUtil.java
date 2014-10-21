package main.java.ee.vrak.util;

import java.io.InputStream;

import main.java.ee.vrak.http.HttpFileHandler;

public class ClasspathUtil {

	public static InputStream getFileFromClasspath(String root) {
		return HttpFileHandler.class.getClassLoader().getResourceAsStream(root);
	}
}
