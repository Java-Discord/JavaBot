package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.javadiscord.javabot.Bot;

/**
 * Listens for reactions in #looking-for-programmer.
 * Automatically deletes messages below a certain score.
 */
public class JobChannelVoteListener extends MessageVoteListener {
	@Override
	protected TextChannel getChannel(Guild guild) {
		return Bot.getConfig().get(guild).getModerationConfig().getJobChannel();
	}

	@Override
	protected int getMessageDeleteVoteThreshold(Guild guild) {
		return Bot.getConfig().get(guild).getModerationConfig().getJobChannelMessageDeleteThreshold();
	}

	@Override
	protected boolean shouldAddInitialEmotes(Guild guild) {
		return false;
	}
}
