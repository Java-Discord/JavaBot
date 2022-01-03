package net.javadiscord.javabot.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class ImageGenerationUtils {

	protected final Path CACHE_PATH = Path.of("imageCache");

	/**
	 * Caches an image by saving it in the CACH_PATH directory.
	 * @param image The image to cache.
	 * @param name The name of the image.
	 * @param formatName The format of the image.
	 * @return The saved File.
	 * @throws IOException If an error occurs.
	 */
	protected File cacheImage(BufferedImage image, String name, String formatName) throws IOException {
		if (Files.notExists(CACHE_PATH)) Files.createDirectory(CACHE_PATH);
		File file = new File(CACHE_PATH.toFile(), name);
		ImageIO.write(image, formatName, file);
		log.info("Successfully cached image: {}/{}", CACHE_PATH, name);
		return file;
	}

	/**
	 * Gets an image from the CACHE_PATH Path by its name.
	 * @param name The name of the image.
	 * @return A {@link BufferedImage}.
	 * @throws IOException If an error occurs.
	 */
	protected BufferedImage getCachedImage(String name) throws IOException {
		if (Files.notExists(CACHE_PATH)) Files.createDirectory(CACHE_PATH);
		return ImageIO.read(new File(CACHE_PATH.toFile(), name));
	}

	/**
	 * Checks if an Image is cached by checking if it exists.
	 * @param name The name of the image.
	 * @return Whether the image exists or not.
	 */
	protected boolean isCached(String name) {
		return new File(CACHE_PATH.toFile(), name).exists();
	}

	/**
	 * Gets an Image from the specified URL.
	 * @param url The url of the image.
	 * @return The image as a {@link BufferedImage}
	 * @throws IOException If an error occurs.
	 */
	protected BufferedImage getImageFromUrl(String url) throws IOException {
		return ImageIO.read(new URL(url));
	}

	/**
	 * Gets an Image from the specified Resource Path.
	 * @param path The path of the image.
	 * @return The image as a {@link BufferedImage}
	 * @throws IOException If an error occurs.
	 */
	protected BufferedImage getResourceImage(String path) throws IOException {
		return ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path)));
	}

	/**
	 * Gets a Font from the specified Resource Path.
	 * @param path The path of the font.
	 * @return The font as an {@link Optional}
	 */
	protected Optional<Font> getResourceFont(String path, float size) {
		Font font = null;
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path))).deriveFont(size);
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
		} catch (Exception e) {
			log.warn("Could not load Font from path " + path);
		}
		return Optional.ofNullable(font);
	}
}
