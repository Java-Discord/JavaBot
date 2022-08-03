package net.javadiscord.javabot.api.routes.qotw_leaderboard.model;

import lombok.Data;

import java.util.List;

@Data
public class QOTWLeaderboardData {
	private List<QOTWMemberData> accounts;
}
