package com.javadiscord.javabot.economy.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Transaction {
	private long id;
	private LocalDateTime createdAt;
	private Long fromUserId;
	private Long toUserId;
	private long value;
}
