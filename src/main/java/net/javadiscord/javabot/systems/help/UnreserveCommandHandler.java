package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.data.config.guild.HelpConfig;

/**
 * A simple command that can be used inside reserved help channels to
 * immediately unreserve them, instead of waiting for a timeout.
 */
public class UnreserveCommandHandler implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var channel = event.getTextChannel();
		var config = Bot.config.get(event.getGuild()).getHelp();
		var channelManager = new HelpChannelManager(config);
		var owner = channelManager.getReservedChannelOwner(channel);
		if (isEligibleToBeUnreserved(event, channel, config, owner)) {
			var reasonOption = event.getOption("reason");
			String reason = (reasonOption == null) ? null : reasonOption.getAsString();
			channelManager.unreserveChannelByUser(channel, owner, reason, event);
			return event.deferReply(true);
		} else {
			return Responses.warning(event, "Could not unreserve this channel. This command only works in help channels you've reserved.");
		}
	}

	private boolean isEligibleToBeUnreserved(SlashCommandInteractionEvent event, TextChannel channel, HelpConfig config, User owner) {
		return channelIsInReservedCategory(channel, config) &&
				(isUserWhoReservedChannel(event, owner) || memberHasHelperRole(event) || memberHasStaffRole(event));
	}

	private boolean channelIsInReservedCategory(TextChannel channel, HelpConfig config) {
		return config.getReservedChannelCategory().equals(channel.getParentCategory());
	}

	private boolean isUserWhoReservedChannel(SlashCommandInteractionEvent event, User owner) {
		return owner != null && event.getUser().equals(owner);
	}

	private boolean memberHasStaffRole(SlashCommandInteractionEvent event) {
		return event.getMember() != null &&
				event.getMember().getRoles().contains(Bot.config.get(event.getGuild()).getModeration().getStaffRole());
	}

	private boolean memberHasHelperRole(SlashCommandInteractionEvent event) {
		return event.getMember() != null &&
				event.getMember().getRoles().contains(Bot.config.get(event.getGuild()).getHelp().getHelperRole());
	}
}
