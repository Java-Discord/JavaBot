package net.javadiscord.javabot.data.h2db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.h2.tools.Server;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Class that provides helper methods for dealing with the database.
 */
@Slf4j
public class DbHelper {
	private DbHelper() {
	}

	/**
	 * Initializes the data source that'll be used throughout the bot to access
	 * the database.
	 *
	 * @param config The bot's configuration.
	 * @return The data source.
	 * @throws IllegalStateException If an error occurs and we're unable to
	 *                               start the database.
	 */
	public static @NotNull HikariDataSource initDataSource(@NotNull BotConfig config) {
		// Determine if we need to initialize the schema, before starting up the server.
		boolean shouldInitSchema = shouldInitSchema(config.getSystems().getHikariConfig().getJdbcUrl());

		// Now that we have remembered whether we need to initialize the schema, start up the server.
		Server server;
		try {
			server = Server.createTcpServer("-tcpPort", "9122", "-ifNotExists").start();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, DbHelper.class.getSimpleName());
			throw new IllegalStateException("Cannot start database server.", e);
		}
		var hikariConfig = new HikariConfig();
		var hikariConfigSource = config.getSystems().getHikariConfig();
		hikariConfig.setJdbcUrl(hikariConfigSource.getJdbcUrl());
		hikariConfig.setMaximumPoolSize(hikariConfigSource.getMaximumPoolSize());
		var ds = new HikariDataSource(hikariConfig);
		// Add a shutdown hook to close down the datasource and server when the JVM terminates.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ds.close();
			server.stop();
		}));
		if (shouldInitSchema) {
			try {
				initializeSchema(ds);
			} catch (IOException | SQLException e) {
				ExceptionLogger.capture(e, DbHelper.class.getSimpleName());
				throw new IllegalStateException("Cannot initialize database schema.", e);
			}
		}
		return ds;
	}

	/**
	 * Does an asynchronous database action using the bot's async pool.
	 *
	 * @param consumer The consumer that will use a connection.
	 */
	public static void doDbAction(ConnectionConsumer consumer) {
		Bot.asyncPool.submit(() -> {
			try (var c = Bot.dataSource.getConnection()) {
				consumer.consume(c);
			} catch (SQLException e) {
				ExceptionLogger.capture(e, DbHelper.class.getSimpleName());
			}
		});
	}

	/**
	 * Does an asynchronous database action using the bot's async pool, and
	 * wraps access to the connection behind a data access object that can be
	 * built using the provided dao constructor.
	 *
	 * @param daoConstructor A function to build a DAO using a connection.
	 * @param consumer       The consumer that does something with the DAO.
	 * @param <T>            The type of data access object. Usually some kind of repository.
	 */
	public static <T> void doDaoAction(Function<Connection, T> daoConstructor, DaoConsumer<T> consumer) {
		Bot.asyncPool.submit(() -> {
			try (var c = Bot.dataSource.getConnection()) {
				var dao = daoConstructor.apply(c);
				consumer.consume(dao);
			} catch (SQLException e) {
				ExceptionLogger.capture(e, DbHelper.class.getSimpleName());
			}
		});
	}

	private static boolean shouldInitSchema(String jdbcUrl) {
		var p = Pattern.compile("jdbc:h2:tcp://localhost:\\d+/(.*)");
		var m = p.matcher(jdbcUrl);
		boolean shouldInitSchema = false;
		if (m.find()) {
			String dbFilePath = m.group(1) + ".mv.db";
			if (Files.notExists(Path.of(dbFilePath))) {
				log.info("Database file doesn't exist yet. Initializing schema.");
				shouldInitSchema = true;
			}
		} else {
			throw new IllegalArgumentException("Invalid JDBC URL. Should point to a file.");
		}
		return shouldInitSchema;
	}

	private static void initializeSchema(HikariDataSource dataSource) throws IOException, SQLException {
		InputStream is = DbHelper.class.getClassLoader().getResourceAsStream("database/schema.sql");
		if (is == null) throw new IOException("Could not load schema.sql.");
		List<String> queries = Arrays.stream(new String(is.readAllBytes()).split(";"))
				.filter(s -> !s.isBlank()).toList();
		try (Connection c = dataSource.getConnection()) {
			for (String rawQuery : queries) {
				StringBuilder query = new StringBuilder();
				rawQuery.lines()
						.map(s -> s.strip().stripIndent())
						.forEach(query::append);
				try (Statement stmt = c.createStatement()) {
					stmt.executeUpdate(query.toString());
				}
			}
		}
		log.info("Successfully initialized H2 database.");
	}
}
