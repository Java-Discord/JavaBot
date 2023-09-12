package net.javadiscord.javabot.api.routes.metrics.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API-Data class which contains all necessary information about a guilds'
 * metrics.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MetricsData {
	private long memberCount;
	private long onlineCount;
	private long weeklyMessages;
	private long activeMembers;
}
