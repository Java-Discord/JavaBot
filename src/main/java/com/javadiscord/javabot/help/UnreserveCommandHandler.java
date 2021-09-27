package com.javadiscord.javabot.help;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

/**
 * A simple command that can be used inside reserved help channels to
 * immediately unreserve them, instead of waiting for a timeout.
 */
public class UnreserveCommandHandler implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var channel = event.getTextChannel();
		var config = Bot.config.get(event.getGuild()).getHelp();
		var channelManager = new HelpChannelManager(config);
		var owner = channelManager.getReservedChannelOwner(channel);
		if (
			config.getHelpChannelCategory().equals(channel.getParent()) &&
			channelManager.isReserved(channel) &&
			(event.getUser().equals(owner) || event.getMember().getRoles().contains(Bot.config.get(event.getGuild()).getModeration().getStaffRole()))
		) {
			channelManager.unreserveChannel(channel);
			return Responses.success(event, "Channel Unreserved", "The channel has been unreserved.");
		}
		return Responses.warning(event, "Could not unreserve this channel. This command only works in help channels you've reserved.");
	}
}
