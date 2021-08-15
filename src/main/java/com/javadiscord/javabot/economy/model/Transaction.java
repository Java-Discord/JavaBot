package com.javadiscord.javabot.economy.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

@Data
public class Transaction {
	private long id;
	private LocalDateTime createdAt;
	@Nullable
	private Long fromUserId;
	@Nullable
	private Long toUserId;
	private long value;
	@Nullable
	private String message;
}
