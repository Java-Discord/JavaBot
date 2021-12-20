package net.javadiscord.javabot.data.h2db;

import java.sql.SQLException;

/**
 * Functional interface for defining operations that consume a specified data-
 * access object.
 * @param <T> The type of the data access object.
 */
@FunctionalInterface
public interface DaoConsumer<T> {
	void consume(T dao) throws SQLException;
}
