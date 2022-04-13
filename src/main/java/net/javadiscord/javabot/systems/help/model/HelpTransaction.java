package net.javadiscord.javabot.systems.help.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

/**
 * Data class that represents a single Help Transaction.
 */
@Data
public class HelpTransaction {
	private long id;
	private long recipient;
	private LocalDateTime createdAt;
	private double value;
	@Nullable
	private String message;
}
