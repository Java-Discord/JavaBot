package net.discordjug.javabot.data.h2db;

import lombok.Data;
import org.h2.api.H2Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This class is used inside {@link DatabaseRepository}s. It describes a single
 * column and combines it which their respective Model Class, {@link T}.
 *
 * @param <T> The Model class.
 */
@Data
public final class TableProperty<T> {
	private final String propertyName;
	private final H2Type h2Type;
	private final BiConsumer<T, Object> consumer;
	private final Function<T, Object> function;
	private final boolean key;

	@Contract("_, _, _, _ -> new")
	public static <T> @NotNull TableProperty<T> of(String propertyName, H2Type h2Type, BiConsumer<T, Object> consumer, Function<T, Object> function) {
		return new TableProperty<>(propertyName, h2Type, consumer, function, false);
	}

	@Contract("_, _, _, _, _ -> new")
	public static <T> @NotNull TableProperty<T> of(String propertyName, H2Type h2Type, BiConsumer<T, Object> consumer, Function<T, Object> function, boolean isKey) {
		return new TableProperty<>(propertyName, h2Type, consumer, function, isKey);
	}
}