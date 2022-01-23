package net.javadiscord.javabot.data.config;

import lombok.Getter;

/**
 * Exception class that handles unknown config properties.
 */
@Getter
public class UnknownPropertyException extends Exception {
	private final String propertyName;
	private final Object parentClass;

	/**
	 * Exception that is thrown when a config property does not exist or could not be found.
	 *
	 * @param propertyName The properties' name.
	 * @param parentClass  The parent class.
	 */
	public UnknownPropertyException(String propertyName, Class<?> parentClass) {
		super(String.format("No property named \"%s\" could be found for class %s.", propertyName, parentClass));
		this.propertyName = propertyName;
		this.parentClass = parentClass;
	}
}
