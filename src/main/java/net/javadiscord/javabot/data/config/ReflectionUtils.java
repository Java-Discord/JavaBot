package net.javadiscord.javabot.data.config;

import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility class for resolving JSON files.
 */
@Slf4j
public class ReflectionUtils {
	private static final Map<Class<?>, Function<String, Object>> propertyTypeParsers = new HashMap<>();

	static {
		propertyTypeParsers.put(Integer.class, Integer::parseInt);
		propertyTypeParsers.put(int.class, Integer::parseInt);
		propertyTypeParsers.put(Long.class, Long::parseLong);
		propertyTypeParsers.put(long.class, Long::parseLong);
		propertyTypeParsers.put(Float.class, Float::parseFloat);
		propertyTypeParsers.put(float.class, Float::parseFloat);
		propertyTypeParsers.put(Double.class, Double::parseDouble);
		propertyTypeParsers.put(double.class, Double::parseDouble);
		propertyTypeParsers.put(Boolean.class, Boolean::parseBoolean);
		propertyTypeParsers.put(String.class, s -> s);
	}

	private ReflectionUtils() {
	}

	public static Optional<Pair<Field, Object>> resolveField(String propertyName, Object parent) throws UnknownPropertyException {
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
	public static Pair<Field, Object> resolveField(String[] fieldNames, Object parent) throws UnknownPropertyException {
		if (fieldNames.length == 0) return null;
		try {
			Field field = parent.getClass().getDeclaredField(fieldNames[0]);
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
			log.warn("Reflection error occurred while resolving property " + Arrays.toString(fieldNames) + " of object of type " + parent.getClass().getSimpleName(), e);
			return null;
		}
	}

	/**
	 * Gets a mapping of properties and their type, recursively for the given
	 * type.
	 *
	 * @param parentPropertyName The root property name to append child field
	 *                           names to. This is null for the base case.
	 * @param parentClass        The class to search for properties in.
	 * @return The map of properties and their types.
	 * @throws IllegalAccessException If a field cannot have its value obtained.
	 */
	public static Map<String, Class<?>> getFields(String parentPropertyName, Class<?> parentClass) throws IllegalAccessException {
		Map<String, Class<?>> fieldsMap = new HashMap<>();
		for (var field : parentClass.getDeclaredFields()) {
			// Skip transient fields.
			if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) continue;
			field.setAccessible(true);
			String fieldPropertyName = parentPropertyName == null ? field.getName() : parentPropertyName + "." + field.getName();
			// Check if the field represents a "leaf" property, one which does not have any children.
			if (propertyTypeParsers.containsKey(field.getType())) {
				fieldsMap.put(fieldPropertyName, field.getType());
			} else {
				var childFieldsMap = getFields(fieldPropertyName, field.getType());
				fieldsMap.putAll(childFieldsMap);
			}
		}
		return fieldsMap;
	}

	/**
	 * Sets the value of a field to a certain value, using {@link ReflectionUtils#propertyTypeParsers}
	 * to try and parse the correct value.
	 *
	 * @param field  The field to set.
	 * @param parent The object whose property value to set.
	 * @param s      The string representation of the value.
	 * @throws IllegalAccessException If the field cannot be set.
	 */
	public static void set(Field field, Object parent, String s) throws IllegalAccessException {
		var parser = propertyTypeParsers.get(field.getType());
		if (parser == null) {
			throw new IllegalArgumentException("No supported property type parser for the type " + field.getType().getSimpleName());
		}
		field.set(parent, parser.apply(s));
	}
}
