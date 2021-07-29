package com.javadiscord.javabot.economy.model;

import lombok.Data;

@Data
public class Account {
	private long userId;
	private long balance;

	public void updateBalance(long change) {
		this.balance += change;
	}
}
