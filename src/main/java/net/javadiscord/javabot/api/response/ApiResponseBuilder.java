package net.javadiscord.javabot.api.response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.javadiscord.javabot.api.gson.GsonColorAdapter;
import net.javadiscord.javabot.api.gson.GsonLocalDateTimeAdapter;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class used to build JSON responses.
 */
public class ApiResponseBuilder {

	private final Gson gson;
	private final Map<String, Object> params;

	/**
	 * The constructor of this class which creates a new {@link Gson}
	 * and initializes the {@link ApiResponseBuilder#params}.
	 */
	public ApiResponseBuilder() {
		gson = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter())
				.registerTypeAdapter(Color.class, new GsonColorAdapter())
				.create();
		params = new HashMap<>();
	}

	public ApiResponseBuilder add(String param, Object value) {
		params.put(param, value);
		return this;
	}

	public String build() {
		return gson.toJson(params);
	}
}
