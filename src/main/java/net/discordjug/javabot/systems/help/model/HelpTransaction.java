package net.discordjug.javabot.systems.help.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data class that represents a single Help Transaction.
 */
@Data
public class HelpTransaction {
	private long id;
	private long recipient;
	private LocalDateTime createdAt;
	private double weight;
	private int messageType;
	private long channelId = -1;
}
