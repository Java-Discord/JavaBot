package net.javadiscord.javabot.systems.help.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Simple Data class that represents a single help channel reservation.
 */
@Data
@AllArgsConstructor
public class ChannelReservation {
	private Long id;
	private long channelId;
	private LocalDateTime reservedAt;
	private long userId;
	private int timeout;
}
