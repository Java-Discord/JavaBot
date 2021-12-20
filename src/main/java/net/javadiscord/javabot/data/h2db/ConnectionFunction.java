package net.javadiscord.javabot.data.h2db;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionFunction<T> {
	T apply(Connection c) throws SQLException;
}
