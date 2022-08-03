package net.javadiscord.javabot.api.model;

import lombok.Data;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;

import java.util.List;

@Data
public class QOTWLeaderboardData {
	private List<QOTWAccount> accounts;
}
