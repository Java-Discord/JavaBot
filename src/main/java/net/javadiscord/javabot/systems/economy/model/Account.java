package net.javadiscord.javabot.systems.economy.model;

import lombok.Data;

/**
 * Simple data class that represents a single economy account.
 */
@Data
public class Account {
	private long userId;
	private long balance;

	public void updateBalance(long change) {
		this.balance += change;
	}
}
