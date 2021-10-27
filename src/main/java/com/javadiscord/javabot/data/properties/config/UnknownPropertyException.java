package com.javadiscord.javabot.data.properties.config;

import lombok.Getter;

@Getter
public class UnknownPropertyException extends Exception {
	private final String propertyName;
	private final Object parentClass;

	public UnknownPropertyException(String propertyName, Class<?> parentClass) {
		super(String.format("No property named \"%s\" could be found for class %s.", propertyName, parentClass));
		this.propertyName = propertyName;
		this.parentClass = parentClass;
	}
}
