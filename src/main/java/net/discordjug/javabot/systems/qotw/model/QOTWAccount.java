package net.discordjug.javabot.systems.qotw.model;

import lombok.Data;

/**
 * Simple data class that represents a single QOTW Account.
 */
@Data
public class QOTWAccount {
	private long userId;
	private long points;
}
