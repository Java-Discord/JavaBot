package net.javadiscord.javabot.listener;

import java.util.concurrent.ExecutorService;

import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.data.config.BotConfig;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;

/**
 * Listens for reactions in #looking-for-programmer.
 * Automatically deletes messages below a certain score.
 */
public class JobChannelVoteListener extends ForumPostVoteListener {

	public JobChannelVoteListener(BotConfig botConfig, ExecutorService asyncPool) {
		super(botConfig, asyncPool);
	}

	@Override
	protected ForumChannel getChannel(Guild guild) {
		return botConfig.get(guild).getModerationConfig().getJobChannel();
	}

	@Override
	protected int getMessageDeleteVoteThreshold(Guild guild) {
		return botConfig.get(guild).getModerationConfig().getJobChannelMessageDeleteThreshold();
	}

	@Override
	protected boolean shouldAddInitialEmotes(Guild guild) {
		return false;
	}
}
