package net.javadiscord.javabot.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for generating images.
 */
@Slf4j
public class ImageGenerationUtils {
	private ImageGenerationUtils() {}

	/**
	 * Gets an Image from the specified URL.
	 *
	 * @param url The url of the image.
	 * @return The image as a {@link BufferedImage}
	 * @throws IOException If an error occurs.
	 */
	public static BufferedImage getImageFromUrl(String url) throws IOException {
		return ImageIO.read(new URL(url));
	}

	/**
	 * Gets an Image from the specified Resource Path.
	 *
	 * @param path The path of the image.
	 * @return The image as a {@link BufferedImage}
	 * @throws IOException If an error occurs.
	 */
	public static BufferedImage getResourceImage(String path) throws IOException {
		return ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path)));
	}

	/**
	 * Gets a Font from the specified Resource Path.
	 *
	 * @param path The path of the font.
	 * @param size The font's size.
	 * @return The font as an {@link Optional}
	 */
	public static Optional<Font> getResourceFont(String path, float size) {
		Font font = null;
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path))).deriveFont(size);
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
		} catch (IOException | FontFormatException e) {
			log.warn("Could not load Font from path " + path);
		}
		return Optional.ofNullable(font);
	}
}
