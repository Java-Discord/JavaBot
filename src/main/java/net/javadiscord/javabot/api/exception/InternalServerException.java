package net.javadiscord.javabot.api.exception;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * An exception which is thrown for invalid JDA entity ids.
 */
public class InternalServerException extends BeansException {

	public InternalServerException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
