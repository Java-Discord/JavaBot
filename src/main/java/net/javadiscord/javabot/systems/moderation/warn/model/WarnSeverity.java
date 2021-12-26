package net.javadiscord.javabot.systems.moderation.warn.model;

public enum WarnSeverity {
	/**
	 * Low severity is intended for small violations.
	 */
	LOW(10),

	/**
	 * Medium severity is for more egregious, but still mostly harmless violations.
	 */
	MEDIUM(20),

	/**
	 * High severity indicates the user did something intentionally malicious.
	 */
	HIGH(40);

	/**
	 * A default weight that is used as a fallback in any case where it is
	 * impossible to obtain the actual weight for a warning, like if a warning
	 * document contains an unknown severity value.
	 */
	public static final int DEFAULT_WEIGHT = 20;

	private final int weight;

	/**
	 * Constructs the value.
	 * @param weight The weight to use.
	 */
	WarnSeverity(int weight) {
		this.weight = weight;
	}

	/**
	 * Gets the weight based on the severity.
	 */
	public int getWeight() {
		return this.weight;
	}

	/**
	 * Gets the weight for a given severity name.
	 * @param name The name of the severity level.
	 * @return The weight for the given severity, or {@link #DEFAULT_WEIGHT} if
	 * no matching severity could be found.
	 */
	public static int getWeightOrDefault(String name) {
		for (var v : values()) {
			if (v.name().equalsIgnoreCase(name)) {
				return v.weight;
			}
		}
		return DEFAULT_WEIGHT;
	}
}
