package net.javadiscord.javabot.api.routes.metrics.model;

import lombok.Data;

/**
 * API-Data class which contains all necessary information about a guilds'
 * metrics.
 */
@Data
public class MetricsData {
	private long memberCount;
	private long onlineCount;

}
