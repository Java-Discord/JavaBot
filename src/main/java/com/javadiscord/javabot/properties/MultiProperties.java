package com.javadiscord.javabot.properties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * This class is an extension of the standard Java properties, and supports
 * loading properties from multiple sources.
 */
public class MultiProperties extends Properties {
	/**
	 * Initializes the properties from the list of paths, where each path should
	 * point to a properties file. <strong>Properties in paths specified last
	 * take precedence over those specified first.</strong>
	 * <p>
	 *     For example, suppose this object is constructed from two properties
	 *     files, in this order:
	 *     <ol>
	 *         <li>props1.properties</li>
	 *         <li>props2.properties</li>
	 *     </ol>
	 *     If props1 contains <code>x=a</code> and props2 contains <code>x=b</code>,
	 *     then calling <code>getProperty("x")</code> on an instance of this
	 *     class with both those files given will return <code>"b"</code>,
	 *     because the later file overrides the earlier one.
	 * </p>
	 * @param paths The list of paths to read properties from.
	 */
	public MultiProperties(Path... paths) {
		for (Path path : paths) {
			Properties props = new Properties();
			try (InputStream is = Files.newInputStream(path)) {
				props.load(is);
			} catch (IOException e) {
				System.err.println("Could not load properties from path: " + path + ", Exception: " + e.getMessage());
			}
			this.putAll(props);
		}
	}

	/**
	 * Gets a path that leads to a classpath resource.
	 * @param name The name of the resource.
	 * @return An optional that will contain a path to the resource, if it was
	 * found.
	 */
	public static Optional<Path> getClasspathResource(String name) {
		URL url = Thread.currentThread().getContextClassLoader().getResource(name);
		if (url == null) return Optional.empty();
		try {
			String[] parts = url.toString().split("!");
			if (parts.length == 2) {
				FileSystem fs = FileSystems.newFileSystem(URI.create(parts[0]), Map.of());
				return Optional.of(fs.getPath(parts[1]));
			}
			return Optional.of(Path.of(url.toURI()));
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
