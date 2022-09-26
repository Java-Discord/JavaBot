package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.List;

/**
 * Defines an analysis that can be performed on a list of messages and semantic
 * data obtained from a reserved help channel, possibly in order to provide
 * contextual help or guidance to the owner of the channel.
 */
public interface ChannelSemanticCheck {
	/**
	 * Performs a check on the given data.
	 *
	 * @param channel      The reserved help channel.
	 * @param owner        The user who reserved the help channel.
	 * @param messages     The list of messages sent in the channel since the user
	 *                     reserved it, ordered from newest to oldest.
	 * @param semanticData Extra semantic data that may be useful in determining
	 *                     when to do things.
	 * @return A rest action that completes when this check is done.
	 */
	RestAction<?> doCheck(TextChannel channel, User owner, List<Message> messages, ChannelSemanticData semanticData);
}
