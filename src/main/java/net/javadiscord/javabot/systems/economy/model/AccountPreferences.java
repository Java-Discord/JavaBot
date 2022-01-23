package net.javadiscord.javabot.systems.economy.model;

import lombok.Data;

/**
 * Simple data class that represents a users account preferences.
 */
@Data
public class AccountPreferences {
	private long userId;
	private boolean receiveTransactionDms;
}
