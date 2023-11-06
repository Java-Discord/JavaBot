package net.discordjug.javabot.data.h2db;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.util.ExceptionLogger;

import java.sql.*;
import java.util.Optional;

import javax.sql.DataSource;

/**
 * Utility that provides some convenience methods for performing database
 * actions.
 */
@Service
@RequiredArgsConstructor
public class DbActions {
	@Getter
	private final DataSource dataSource;

	/**
	 * Maps a query.
	 *
	 * @param query    The query.
	 * @param modifier The {@link StatementModifier}.
	 * @param mapper   The {@link ResultSetMapper}.
	 * @param <T>      The generic type.
	 * @return A generic type.
	 * @throws SQLException If an error occurs.
	 */
	public <T> T mapQuery(@NotNull String query, @NotNull StatementModifier modifier, @NotNull ResultSetMapper<T> mapper) throws SQLException {
		try (Connection c = dataSource.getConnection(); PreparedStatement stmt = c.prepareStatement(query)) {
			modifier.modify(stmt);
			ResultSet rs = stmt.executeQuery();
			return mapper.map(rs);
		}
	}

	/**
	 * Gets a count, using a query which <strong>must</strong> return a long
	 * integer value as the first column of the result set.
	 *
	 * @param query    The query.
	 * @param modifier A modifier to use to set parameters for the query.
	 * @return The column value.
	 */
	public long count(@NotNull String query, @NotNull StatementModifier modifier) {
		try (Connection c = dataSource.getConnection(); PreparedStatement stmt = c.prepareStatement(query)) {
			modifier.modify(stmt);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) return 0;
			return rs.getLong(1);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, DbActions.class.getSimpleName());
			return 0;
		}
	}

	/**
	 * Gets a count, using a query which <strong>must</strong> return a long
	 * integer value as the first column of the result set.
	 *
	 * @param query The query.
	 * @return The column value.
	 */
	public long count(@NotNull String query) {
		try (
				Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()
		) {
			ResultSet rs = stmt.executeQuery(query);
			if (!rs.next()) return 0;
			return rs.getLong(1);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, DbActions.class.getSimpleName());
			return 0;
		}
	}

	/**
	 * Updates a database table.
	 *
	 * @param query  The query.
	 * @param params The queries' parameters.
	 * @return The rows that got updates during this process.
	 * @throws SQLException If an error occurs.
	 */
	public int update(@NotNull String query, Object @NotNull ... params) throws SQLException {
		try (Connection c = dataSource.getConnection(); PreparedStatement stmt = c.prepareStatement(query)) {
			int i = 1;
			for (Object param : params) {
				stmt.setObject(i++, param);
			}
			return stmt.executeUpdate();
		}
	}

	/**
	 * Fetches a single result from the database.
	 *
	 * @param query    The query to use.
	 * @param modifier The query modifier for setting parameters.
	 * @param mapper   The result set mapper. It is assumed to already have its
	 *                 cursor on the first row. Do not call next() on it.
	 * @param <T>      The result type.
	 * @return An optional that may contain the result, if one was found.
	 */
	public <T> Optional<T> fetchSingleEntity(String query, StatementModifier modifier, ResultSetMapper<T> mapper) {
		try {
			return mapQuery(query, modifier, rs -> {
				if (!rs.next()) return Optional.empty();
				return Optional.of(mapper.map(rs));
			});
		} catch (SQLException e) {
			ExceptionLogger.capture(e, DbActions.class.getSimpleName());
			return Optional.empty();
		}
	}

	/**
	 * Gets the logical size of a single database table in bytes.
	 *
	 * @param table The database table.
	 * @return The logical size, in bytes.
	 */
	public int getLogicalSize(String table) {
		try (Connection c = dataSource.getConnection(); PreparedStatement stmt = c.prepareStatement("CALL DISK_SPACE_USED(?)")) {
			stmt.setString(1, table);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, DbActions.class.getSimpleName());
		}
		return 0;
	}
}
