package net.javadiscord.javabot.command;

import lombok.Getter;

/**
 * An exception that can be thrown while responding to a user.
 */
public class ResponseException extends Exception {
	@Getter
	private final Type type;

	public ResponseException(Type type, String message, Throwable cause) {
		super(message, cause);
		this.type = type;
	}

	public static ResponseException warning(String message) {
		return new ResponseException(Type.WARNING, message, null);
	}

	public static ResponseException error(String message, Throwable cause) {
		return new ResponseException(Type.ERROR, message, cause);
	}

	/**
	 * Enum class representing all Response Exception types.
	 */
	public enum Type {WARNING, ERROR}
}
