package com.javadiscord.javabot.data.h2db;

import com.javadiscord.javabot.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

/**
 * Data source that is used to obtain connections to the database.
 */
public class H2DataSource {
	private static final Logger log = LoggerFactory.getLogger(H2DataSource.class);

	/**
	 * Initializes the database if it does not exist yet, using the schema SQL
	 * script provided in the resources.
	 */
	public void initDatabase() throws IOException, SQLException {
		if (!databaseFileExists()) {
			log.info("H2 database doesn't exist yet. Initializing it now.");
			InputStream is = getClass().getResourceAsStream("/schema.sql");
			if (is == null) throw new IOException("Could not load schema.sql.");
			try (Connection con = this.getConnection();
					Statement statement = con.createStatement()) {
				String sql = new String(is.readAllBytes());
				for (String query : sql.split(";")) {
					statement.executeUpdate(query);
				}
			}
			log.info("Successfully initialized H2 database.");
		} else {
			log.info("H2 database exists, ready to create connections.");
		}
	}

	public Connection getConnection() throws SQLException {
		String fileName = Bot.config.getSystems().getH2DatabaseFileName();
		return DriverManager.getConnection("jdbc:h2:file:./" + fileName + ";AUTO_SERVER=TRUE");
	}

	/**
	 * Determines if a database file whose name matches the configured file name
	 * exists.
	 * @return True if the database file exists, or false otherwise.
	 * @throws IOException If an error occurs while searching for the file.
	 */
	private boolean databaseFileExists() throws IOException {
		String fileName = Bot.config.getSystems().getH2DatabaseFileName();
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fileName + ".*");
		try(Stream<Path> stream = Files.walk(Path.of(""))){
			return stream.anyMatch(matcher::matches);
		}
	}
}
