package com.javadiscord.javabot.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseHelper {
	private static final Map<String, String> SQL_CACHE = new ConcurrentHashMap<>();

	public static String loadSql(String resourceName) throws IOException {
		String sql = SQL_CACHE.get(resourceName);
		if (sql == null) {
			InputStream is = DatabaseHelper.class.getResourceAsStream(resourceName);
			if (is == null) throw new IOException("Could not load " + resourceName);
			sql = new String(is.readAllBytes());
			SQL_CACHE.put(resourceName, sql);
		}
		return sql;
	}
}
