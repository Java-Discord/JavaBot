package net.discordjug.javabot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.discordjug.javabot.data.h2db.DbHelper;
import net.discordjug.javabot.data.h2db.commands.MigrateSubcommand;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

public class DatabaseTest {
	@Test
	void testCreateDatabaseFromSchema() throws SQLException, IOException {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setUrl("jdbc:h2:mem:"+UUID.randomUUID().toString());
		DbHelper.initializeSchema(ds);
	}
	
	@Test
	void testDatabaseFilesCorrect() throws URISyntaxException, IOException {
		Set<String> expectedMigrations;
		try (Stream<Path> list = Files.list(Path.of("src/main/resources/database/migrations"))) {
			expectedMigrations = list
					.map(path -> path.getFileName().toString())
					.filter(file -> file.endsWith(".sql"))
					.collect(Collectors.toSet());
		}
		Set<String> foundMigrations = MigrateSubcommand.getAvailableMigrations().stream()
				.map(Choice::getName)
				.collect(Collectors.toSet());
		assertEquals(expectedMigrations, foundMigrations);
	}
}
