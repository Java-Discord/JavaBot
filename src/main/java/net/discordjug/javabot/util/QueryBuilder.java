package net.discordjug.javabot.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Builds SQL Queries.
 */
public class QueryBuilder {
	private final PreparedStatement stmt;
	private int parameterIndex = 1;

	public QueryBuilder(Connection con, String sql) throws SQLException {
		this.stmt = con.prepareStatement(sql);
	}

	public QueryBuilder setLong(long n) throws SQLException {
		this.stmt.setLong(this.parameterIndex++, n);
		return this;
	}

	public QueryBuilder setString(String s) throws SQLException {
		this.stmt.setString(this.parameterIndex++, s);
		return this;
	}

	public QueryBuilder setTimestamp(LocalDateTime timestamp) throws SQLException {
		this.stmt.setTimestamp(this.parameterIndex++, Timestamp.valueOf(timestamp));
		return this;
	}

	public QueryBuilder setBoolean(boolean b) throws SQLException {
		this.stmt.setBoolean(this.parameterIndex++, b);
		return this;
	}

	public PreparedStatement build() {
		return this.stmt;
	}
}
