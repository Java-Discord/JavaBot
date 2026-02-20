package net.discordjug.javabot.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.discordjug.javabot.data.config.PatternTypeAdapter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.regex.Pattern;

/**
 * Utility class that contains several methods for using Gson.
 */
public class GsonUtils {

	private static final Gson gson = new GsonBuilder()
			.serializeNulls()
			.setPrettyPrinting()
			.registerTypeAdapter(Pattern.class, new PatternTypeAdapter())
			.enableComplexMapKeySerialization()
			.create();

	public static <T> T fromJson(Reader json, Class<T> type) {
		return gson.fromJson(json, type);
	}

	public static Object fromJson(String json, Type type) {
		return gson.fromJson(json, type);
	}

	public static String toJson(Object object) {
		return gson.toJson(object);
	}

	public static void toJson(Object object, Writer writer) {
		gson.toJson(object, writer);
	}
}
