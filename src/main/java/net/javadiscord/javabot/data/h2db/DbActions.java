package net.javadiscord.javabot.data.h2db;

import net.javadiscord.javabot.Bot;

import java.sql.Connection;
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
	private DbActions () {}

	public static void doAction(ConnectionConsumer consumer) throws SQLException {
		try (var c = Bot.dataSource.getConnection()) {
			consumer.consume(c);
		}
	}

	public static <T> T map(ConnectionFunction<T> function) throws SQLException {
		try (var c = Bot.dataSource.getConnection()) {
			return function.apply(c);
		}
	}

	public static <T> T mapQuery(String query, StatementModifier modifier, ResultSetMapper<T> mapper) throws SQLException {
		try (var c = Bot.dataSource.getConnection(); var stmt = c.prepareStatement(query)) {
			modifier.modify(stmt);
			var rs = stmt.executeQuery();
			return mapper.map(rs);
		}
	}

	public static <T> CompletableFuture<T> mapQueryAsync(String query, StatementModifier modifier, ResultSetMapper<T> mapper) {
		CompletableFuture<T> cf = new CompletableFuture<>();
		Bot.asyncPool.submit(() -> {
			try {
				cf.complete(mapQuery(query, modifier, mapper));
			} catch (SQLException e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	public static long count(String query, StatementModifier modifier) {
		try (var c = Bot.dataSource.getConnection(); var stmt = c.prepareStatement(query)) {
			modifier.modify(stmt);
			var rs = stmt.executeQuery();
			if (!rs.next()) return 0;
			return rs.getLong(1);
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

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
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	/**
	 * Does an asynchronous database action using the bot's async pool, and
	 * wraps access to the connection behind a data access object that can be
	 * built using the provided dao constructor.
	 * @param daoConstructor A function to build a DAO using a connection.
	 * @param consumer The consumer that does something with the DAO.
	 * @param <T> The type of data access object. Usually some kind of repository.
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
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	public static <T> CompletableFuture<T> mapAsync(ConnectionFunction<T> function) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Bot.asyncPool.submit(() -> {
			try (var c = Bot.dataSource.getConnection()) {
				future.complete(function.apply(c));
			} catch (SQLException e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	/**
	 * Fetches a single result from the database.
	 * @param query The query to use.
	 * @param modifier The query modifier for setting parameters.
	 * @param mapper The result set mapper. It is assumed to already have its
	 *               cursor on the first row. Do not call next() on it.
	 * @param <T> The result type.
	 * @return An optional that may contain the result, if one was found.
	 */
	public static <T> Optional<T> fetchSingleEntity(String query, StatementModifier modifier, ResultSetMapper<T> mapper) {
		try {
			return mapQuery(query, modifier, rs -> {
				if (!rs.next()) return Optional.empty();
				return Optional.of(mapper.map(rs));
			});
		} catch (SQLException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
