package net.javadiscord.javabot.api.gson;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {

	@Override
	public Color deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		return Color.decode(jsonElement.getAsString());
	}

	@Override
	public JsonPrimitive serialize(@NotNull Color color, Type type, JsonSerializationContext jsonSerializationContext) {
		return new JsonPrimitive("#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase());
	}
}
