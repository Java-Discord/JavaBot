package com.javadiscord.javabot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple utility for reading string contents from resources on the classpath,
 * and caching them for the duration of the runtime.
 */
public class StringResourceCache {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

	public static String load(String resourceName) {
		String sql = CACHE.get(resourceName);
		if (sql == null) {
			InputStream is = StringResourceCache.class.getResourceAsStream(resourceName);
			if (is == null) throw new RuntimeException("Could not load " + resourceName);
			try {
				sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			CACHE.put(resourceName, sql);
		}
		return sql;
	}
}
