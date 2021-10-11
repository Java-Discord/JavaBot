package com.javadiscord.javabot.properties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

}
