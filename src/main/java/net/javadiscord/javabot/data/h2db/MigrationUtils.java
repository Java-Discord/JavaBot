package net.javadiscord.javabot.data.h2db;

import net.javadiscord.javabot.data.h2db.commands.MigrationsListSubcommand;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

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
		var resource = MigrationsListSubcommand.class.getResource("/migrations/");
		if (resource == null) throw new IOException("Missing resource /migrations/");
		var uri = resource.toURI();
		Path dirPath;
		try {
			dirPath = Paths.get(uri);
		} catch (FileSystemNotFoundException e) {
			var env = new HashMap<String, String>();
			dirPath = FileSystems.newFileSystem(uri, env).getPath("/migrations/");
		}
		return dirPath;
	}
}
