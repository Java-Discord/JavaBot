package com.javadiscord.javabot.service.help.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChannelReservation {
	private Long id;
	private long channelId;
	private LocalDateTime reservedAt;
	private long userId;
	private int timeout;
}
