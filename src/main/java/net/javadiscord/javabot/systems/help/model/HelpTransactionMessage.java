package net.javadiscord.javabot.systems.help.model;

/**
 * Enum class that represents the different types of transactions messages.
 */
public enum HelpTransactionMessage {
	/**
	 * Unknown transaction message.
	 */
	UNKNOWN,
	/**
	 * Used for transactions regarding standard help channel helping.
	 */
	HELPED,
	/**
	 * Used for transactions regarding one being thanked.
	 */
	GOT_THANKED,
	/**
	 * Used for transactions regarding thanking others.
	 */
	THANKED_USER,
	/**
	 * Used for transactions regarding the daily experience subtraction.
	 */
	DAILY_SUBTRACTION
}
