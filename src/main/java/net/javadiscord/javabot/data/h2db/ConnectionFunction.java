package net.javadiscord.javabot.data.h2db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for connecting to the H2 SQL Database.
 * @param <T> The generic type that is returned.
 */
@FunctionalInterface
public interface ConnectionFunction<T> {
	T apply(Connection c) throws SQLException;
}
