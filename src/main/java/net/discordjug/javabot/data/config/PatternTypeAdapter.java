package net.discordjug.javabot.data.config;

import java.io.IOException;
import java.util.regex.Pattern;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * A gson {@link TypeAdapter} that allows serializing and deserializing regex {@link Pattern}s.
 */
public class PatternTypeAdapter extends TypeAdapter<Pattern> {

	@Override
	public void write(JsonWriter writer, Pattern value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}
		writer.value(value.toString());
	}

	@Override
	public Pattern read(JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
		}
		String value = reader.nextString();
		return Pattern.compile(value);
	}

}
