package net.javadiscord.javabot.systems.economy.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

/**
 * Simple data class that represents a single Transaction.
 */
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
