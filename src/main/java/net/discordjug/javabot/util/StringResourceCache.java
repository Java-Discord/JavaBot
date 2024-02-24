package net.discordjug.javabot.util;

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

	private StringResourceCache() {
	}

	/**
	 * Loads a String from the resources folder.
	 *
	 * @param resourceName The resources' name & path.
	 * @return The resources' content as a String.
	 */
	public static String load(String resourceName) {
		String content = CACHE.get(resourceName);
		if (content == null) {
			try(InputStream is = StringResourceCache.class.getResourceAsStream(resourceName)){
				if (is == null) throw new RuntimeException("Could not load " + resourceName);
				content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}catch (IOException e) {
				ExceptionLogger.capture(e, StringResourceCache.class.getSimpleName());
				throw new UncheckedIOException(e);
			}
			
			CACHE.put(resourceName, content);
		}
		return content;
	}
}
