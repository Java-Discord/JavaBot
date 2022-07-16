package net.javadiscord.javabot.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for caching images.
 */
@Slf4j
public class ImageCache {
	private static final Map<String, BufferedImage> cache;

	static {
		cache = new HashMap<>();
	}

	/**
	 * Caches an image by saving it in a {@link Map}.
	 *
	 * @param image The image to cache.
	 * @param name  The name of the image.
	 */
	public static void cacheImage(String name, BufferedImage image) {
		log.info("Added Image to Cache: {}", name);
		cache.put(name, image);
	}

	/**
	 * Gets an image from the {@link Map}.
	 *
	 * @param name The name of the image.
	 * @return A {@link BufferedImage}.
	 */
	public static BufferedImage getCachedImage(String name) {
		log.info("Retrieved Image from Cache: {}", name);
		return cache.get(name);
	}

	/**
	 * Removes an image from the {@link Map} whose name contains the specified keyword.
	 *
	 * @param keyword The keyword.
	 * @return A {@link BufferedImage}.
	 */
	public static boolean removeCachedImagesByKeyword(String keyword) {
		return cache.keySet().removeIf(s -> s.contains(keyword));
	}

	/**
	 * Checks if an Image is cached.
	 *
	 * @param name The name of the image.
	 * @return Whether the image is already cached or not.
	 */
	public static boolean isCached(String name) {
		return cache.containsKey(name);
	}
}
