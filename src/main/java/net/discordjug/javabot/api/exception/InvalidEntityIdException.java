package net.discordjug.javabot.api.exception;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * An exception which is thrown for invalid JDA entity ids.
 */
public class InvalidEntityIdException extends BeansException {
	@Nullable
	private final Class<?> requiredEntity;

	public InvalidEntityIdException(Class<?> requiredEntity, String msg) {
		super(msg);
		this.requiredEntity = requiredEntity;
	}

	public Class<?> getRequiredEntity() {
		return requiredEntity;
	}
}
