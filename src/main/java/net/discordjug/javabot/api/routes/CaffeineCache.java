package net.discordjug.javabot.api.routes;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.Getter;

/**
 * Simple parent class which enables all extending classes to have their own
 * {@link Cache}.
 *
 * @param <K> The caches' key.
 * @param <V> The caches' value.
 */
public abstract class CaffeineCache<K, V> {

	@Getter
	private final Cache<K, V> cache;

	protected CaffeineCache(Cache<K, V> cache) {
		this.cache = cache;
	}
}
