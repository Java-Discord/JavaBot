package com.javadiscord.javabot.properties.config;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
public class QOTWConfig {
	private boolean dmEnabled;
	private long submissionChannelId;

	public TextChannel getSubmissionChannel(Guild guild) {
		return guild.getTextChannelById(this.submissionChannelId);
	}
}
