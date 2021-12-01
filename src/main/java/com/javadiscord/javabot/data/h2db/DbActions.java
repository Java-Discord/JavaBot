package com.javadiscord.javabot.data.h2db;

import com.javadiscord.javabot.Bot;

import java.sql.Connection;
import java.sql.SQLException;
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
}
