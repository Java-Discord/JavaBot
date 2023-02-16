package net.javadiscord.javabot.listener;

import java.util.concurrent.ExecutorService;

import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.data.config.BotConfig;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;

/**
 * Listens for messages and reactions in #share-knowledge.
 * Automatically deletes messages below a certain score.
 */
public class ShareKnowledgeVoteListener extends ForumPostVoteListener {
	public ShareKnowledgeVoteListener(BotConfig botConfig, ExecutorService asyncPool) {
		super(botConfig, asyncPool);
	}

	@Override
	protected ForumChannel getChannel(Guild guild) {
		return botConfig.get(guild).getModerationConfig().getShareKnowledgeChannel();
	}

	@Override
	protected int getMessageDeleteVoteThreshold(Guild guild) {
		return botConfig.get(guild).getModerationConfig().getShareKnowledgeMessageDeleteThreshold();
	}
}
