package net.discordjug.javabot.data.config;

import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.GsonUtils;
import net.discordjug.javabot.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class for resolving JSON files.
 */
@Slf4j
public class ReflectionUtils {
	private ReflectionUtils() {
	}

	public static Optional<Pair<Field, Object>> resolveField(@NotNull String propertyName, Object parent) throws UnknownPropertyException {
		return Optional.ofNullable(resolveField(propertyName.split("\\."), parent));
	}

	/**
	 * Finds a field and the object whose type contains it, for a given parent
	 * object.
	 *
	 * @param fieldNames An array containing an ordered list of the names of all
	 *                   fields to traverse.
	 * @param parent     The object whose fields to traverse.
	 * @return The field and the object upon which it can be applied.
	 * @throws UnknownPropertyException If no field could be resolved.
	 */
	public static @Nullable Pair<Field, Object> resolveField(@NotNull String[] fieldNames, @NotNull Object parent) throws UnknownPropertyException {
		if (fieldNames.length == 0) return null;
		try {
			Field field = findDeclaredField(parent.getClass(), fieldNames[0]);
			// Transient fields should not exist in the context of property resolution, treat them as unknown.
			if (Modifier.isTransient(field.getModifiers())) {
				throw new UnknownPropertyException(fieldNames[0], parent.getClass());
			}
			field.setAccessible(true);
			Object value = field.get(parent);
			if (fieldNames.length == 1) {
				return new Pair<>(field, parent);
			} else if (value != null) {
				return resolveField(Arrays.copyOfRange(fieldNames, 1, fieldNames.length), value);
			} else {
				return null;
			}
		} catch (NoSuchFieldException e) {
			throw new UnknownPropertyException(fieldNames[0], parent.getClass());
		} catch (IllegalAccessException e) {
			ExceptionLogger.capture(e, ReflectionUtils.class.getSimpleName());
			log.warn("Reflection error occurred while resolving property " + Arrays.toString(fieldNames) + " of object of type " + parent.getClass().getSimpleName(), e);
			return null;
		}
	}

	private static Field findDeclaredField(Class<?> cl, String name) throws NoSuchFieldException {
		try {
			return cl.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			for (Field field : cl.getDeclaredFields()) {
				if(field.getName().equalsIgnoreCase(name)) {
					return field;
				}
			}
			throw e;
		}
	}

	/**
	 * Sets the value of a field to a new value.
	 *
	 * @param field  The field to set.
	 * @param parent The object whose property value to set.
	 * @param s      The string representation of the value.
	 * @return Returns the new value.
	 * @throws IllegalAccessException If the field cannot be set.
	 */
	public static Object set(@NotNull Field field, @NotNull Object parent, @NotNull String s) throws IllegalAccessException {
		Object value = field.getType() == String.class ? s : GsonUtils.fromJson(s, field.getGenericType());
		field.set(parent, value);
		return value;
	}
}
