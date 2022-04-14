package net.javadiscord.javabot.systems.help.model;

import lombok.Data;
import net.javadiscord.javabot.util.TimeUtils;

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
	private int messageType;

	public String getMessage() {
		return switch (HelpTransactionMessage.values()[messageType]) {
			case UNKNOWN -> "Unknown";
			case HELPED -> "For helping another user in a reserved help channel";
			case GOT_THANKED -> "For receiving a thank from another user";
			case THANKED_USER -> "For thanking another user";
			case DAILY_SUBTRACTION -> "Daily Experience Subtraction";
		};
	}

	public String format() {
		return String.format("%s%s XP (%s)\n%s", value > 0 ? "+" : "-", value, createdAt.format(TimeUtils.STANDARD_FORMATTER), this.getMessage());
	}
}
