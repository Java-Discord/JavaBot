package net.javadiscord.javabot.systems.economy.model;

import lombok.Data;

@Data
public class AccountPreferences {
	private long userId;
	private boolean receiveTransactionDms;
}
