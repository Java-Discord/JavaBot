package com.javadiscord.javabot.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseHelper {
	private static final Map<String, String> SQL_CACHE = new ConcurrentHashMap<>();

	public static String loadSql(String resourceName) {
		String sql = SQL_CACHE.get(resourceName);
		if (sql == null) {
			InputStream is = DatabaseHelper.class.getResourceAsStream(resourceName);
			if (is == null) throw new RuntimeException("Could not load " + resourceName);
			try {
				sql = new String(is.readAllBytes());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			SQL_CACHE.put(resourceName, sql);
		}
		return sql;
	}
}
