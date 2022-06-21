package net.javadiscord.javabot.data.h2db;

import io.sentry.Sentry;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Utility that provides some convenience methods for performing database
 * actions.
 */
public class DbActions {
	// Hide the constructor.
	private DbActions() {
	}

	/**
	 * Consumes an action based on the given {@link ConnectionConsumer}.
	 *
	 * @param consumer The {@link ConnectionConsumer}.
	 * @throws SQLException If an error occurs.
	 */
	public static void doAction(ConnectionConsumer consumer) throws SQLException {
		try (var c = Bot.dataSource.getConnection()) {
			consumer.consume(c);
		}
	}

	/**
	 * Maps an action based on the given {@link ConnectionFunction}.
	 *
	 * @param function The {@link ConnectionFunction}.
	 * @param <T>      The generic type.
	 * @return A generic type.
	 * @throws SQLException If an error occurs.
	 */
	public static <T> T map(ConnectionFunction<T> function) throws SQLException {
		try (var c = Bot.dataSource.getConnection()) {
			return function.apply(c);
		}
	}

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
	public static <T> T mapQuery(String query, StatementModifier modifier, ResultSetMapper<T> mapper) throws SQLException {
		try (var c = Bot.dataSource.getConnection(); var stmt = c.prepareStatement(query)) {
			modifier.modify(stmt);
			var rs = stmt.executeQuery();
			return mapper.map(rs);
		}
	}

	/**
	 * Maps a query asynchronous.
	 *
	 * @param query    The query.
	 * @param modifier The {@link StatementModifier}.
	 * @param mapper   The {@link ResultSetMapper}.
	 * @param <T>      The generic type.
	 * @return A generic type.
	 */
	public static <T> CompletableFuture<T> mapQueryAsync(String query, StatementModifier modifier, ResultSetMapper<T> mapper) {
		CompletableFuture<T> cf = new CompletableFuture<>();
		Bot.asyncPool.submit(() -> {
			try {
				cf.complete(mapQuery(query, modifier, mapper));
			} catch (SQLException e) {
				ExceptionLogger.capture(e, DbActions.class.getSimpleName());
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	/**
	 * Gets a count, using a query which <strong>must</strong> return a long
	 * integer value as the first column of the result set.
	 *
	 * @param query    The query.
	 * @param modifier A modifier to use to set parameters for the query.
	 * @return The column value.
	 */
	public static long count(String query, StatementModifier modifier) {
		try (var c = Bot.dataSource.getConnection(); var stmt = c.prepareStatement(query)) {
			modifier.modify(stmt);
			var rs = stmt.executeQuery();
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
	 * @param query    The query.
	 * @return The column value.
	 */
	public static long count(String query) {
		try (
				var conn = Bot.dataSource.getConnection();
				var stmt = conn.createStatement()
		) {
			var rs = stmt.executeQuery(query);
			if (!rs.next()) return 0;
			return rs.getLong(1);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, DbActions.class.getSimpleName());
			return 0;
		}
	}

	/**
	 * Convenience method similar to {@link DbActions#count(String, StatementModifier)}
	 * which allows for getting the count from a query using simple string
	 * formatting instead of having to define a statement modifier.
	 * <p>
	 *     <strong>WARNING</strong>: This method should NEVER be called with
	 *     user-provided data.
	 * </p>
	 *
	 * @param queryFormat The format string.
	 * @param args The set of arguments to pass to the formatter.
	 * @return The count.
	 */
	public static long countf(String queryFormat, Object... args) {
		return count(String.format(queryFormat, args));
	}

	/**
	 * Updates a database table.
	 *
	 * @param query  The query.
	 * @param params The queries' parameters.
	 * @return The rows that got updates during this process.
	 * @throws SQLException If an error occurs.
	 */
	public static int update(String query, Object... params) throws SQLException {
		try (var c = Bot.dataSource.getConnection(); var stmt = c.prepareStatement(query)) {
			int i = 1;
			for (var param : params) {
				stmt.setObject(i++, param);
			}
			return stmt.executeUpdate();
		}
	}

	/**
	 * Does an asynchronous database action using the bot's async pool.
	 *
	 * @param consumer The consumer that will use a connection.
	 * @return A future that completes when the action is complete.
	 */
	public static CompletableFuture<Void> doAsyncAction(ConnectionConsumer consumer) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Bot.asyncPool.submit(() -> {
			try (var c = Bot.dataSource.getConnection()) {
				consumer.consume(c);
				future.complete(null);
			} catch (SQLException e) {
				ExceptionLogger.capture(e, DbActions.class.getSimpleName());
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	/**
	 * Does an asynchronous database action using the bot's async pool, and
	 * wraps access to the connection behind a data access object that can be
	 * built using the provided dao constructor.
	 *
	 * @param daoConstructor A function to build a DAO using a connection.
	 * @param consumer       The consumer that does something with the DAO.
	 * @param <T>            The type of data access object. Usually some kind of repository.
	 * @return A future that completes when the action is complete.
	 */
	public static <T> CompletableFuture<Void> doAsyncDaoAction(Function<Connection, T> daoConstructor, DaoConsumer<T> consumer) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Bot.asyncPool.submit(() -> {
			try (var c = Bot.dataSource.getConnection()) {
				var dao = daoConstructor.apply(c);
				consumer.consume(dao);
				future.complete(null);
			} catch (SQLException e) {
				ExceptionLogger.capture(e, DbActions.class.getSimpleName());
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	/**
	 * Maps a {@link ConnectionFunction} asynchronous.
	 *
	 * @param function The {@link ConnectionFunction}.
	 * @param <T>      The generic type.
	 * @return A generic type.
	 */
	public static <T> CompletableFuture<T> mapAsync(ConnectionFunction<T> function) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Bot.asyncPool.submit(() -> {
			try (var c = Bot.dataSource.getConnection()) {
				future.complete(function.apply(c));
			} catch (SQLException e) {
				ExceptionLogger.capture(e, DbActions.class.getSimpleName());
				future.completeExceptionally(e);
			}
		});
		return future;
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
	public static <T> Optional<T> fetchSingleEntity(String query, StatementModifier modifier, ResultSetMapper<T> mapper) {
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
	public static int getLogicalSize(String table) {
		try (var c = Bot.dataSource.getConnection(); var stmt = c.prepareStatement("CALL DISK_SPACE_USED(?)")) {
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
