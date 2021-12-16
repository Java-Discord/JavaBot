package com.javadiscord.javabot.data.h2db;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetMapper<T> {
	T map(ResultSet rs) throws SQLException;
}
