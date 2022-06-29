package net.javadiscord.javabot.data.h2db;

import lombok.Data;
import org.h2.api.H2Type;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Data
public final class TableProperty<T> {
	private final String propertyName;
	private final H2Type h2Type;
	private final BiConsumer<T, Object> consumer;
	private final Function<T, Object> function;
	private final boolean excludeFromInsertion;

	public static <T> TableProperty<T> of(String propertyName, H2Type h2Type, BiConsumer<T, Object> consumer, Function<T, Object> function) {
		return new TableProperty<>(propertyName, h2Type, consumer, function, false);
	}

	public static <T> TableProperty<T> of(String propertyName, H2Type h2Type, BiConsumer<T, Object> consumer, Function<T, Object> function, boolean excludeFromInsertion) {
		return new TableProperty<>(propertyName, h2Type, consumer, function, excludeFromInsertion);
	}
}
