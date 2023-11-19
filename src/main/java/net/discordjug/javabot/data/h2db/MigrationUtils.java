package net.discordjug.javabot.data.h2db;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import net.discordjug.javabot.data.h2db.commands.MigrationsListSubcommand;

/**
 * Utility class that handles SQL Migrations.
 */
public class MigrationUtils {

	private MigrationUtils() {
	}

	/**
	 * Tries to get the Migrations Directories' Path.
	 *
	 * @return The Migrations Directories' Path.
	 * @throws URISyntaxException If an error occurs.
	 * @throws IOException        If an error occurs.
	 */
	public static Path getMigrationsDirectory() throws URISyntaxException, IOException {
		URL resource = MigrationsListSubcommand.class.getResource("/database/migrations/");
		if (resource == null) throw new IOException("Missing resource /migrations/");
		URI uri = resource.toURI();
		try {
			return Path.of(uri);
		} catch (FileSystemNotFoundException e) {
			Map<String, String> env = new HashMap<>();
			FileSystem dir = FileSystems.newFileSystem(uri, env);
			return dir.getPath("/database/migrations/");
		}
	}
}
