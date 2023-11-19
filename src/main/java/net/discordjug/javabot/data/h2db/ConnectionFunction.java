package net.discordjug.javabot.data.h2db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional interface for a function which produces some object using a
 * connection.
 *
 * @param <T> The generic type that is returned.
 */
@FunctionalInterface
public interface ConnectionFunction<T> {
	T apply(Connection c) throws SQLException;
}
