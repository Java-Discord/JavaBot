package net.discordjug.javabot.data.h2db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface for mapping {@link ResultSet}s.
 *
 * @param <T> The generic type.
 */
@FunctionalInterface
public interface ResultSetMapper<T> {
	T map(ResultSet rs) throws SQLException;
}
