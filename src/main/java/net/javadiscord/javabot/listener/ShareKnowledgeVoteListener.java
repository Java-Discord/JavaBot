package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.javadiscord.javabot.Bot;

/**
 * Listens for messages and reactions in #share-knowledge.
 * Automatically deletes messages below a certain score.
 */
public class ShareKnowledgeVoteListener extends MessageVoteListener {
	@Override
	protected TextChannel getChannel(Guild guild) {
		return Bot.getConfig().get(guild).getModerationConfig().getShareKnowledgeChannel();
	}

	@Override
	protected int getMessageDeleteVoteThreshold(Guild guild) {
		return Bot.getConfig().get(guild).getModerationConfig().getShareKnowledgeMessageDeleteThreshold();
	}
}
