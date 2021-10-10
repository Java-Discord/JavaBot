package com.javadiscord.javabot.help;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
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
        if (isEligibleToBeUnreserved(event, channel, config, owner)) {
            channelManager.unreserveChannel(channel).queue();
            return Responses.success(event, "Channel Unreserved", "The channel has been unreserved.");
        }
        return Responses.warning(event, "Could not unreserve this channel. This command only works in help channels you've reserved.");
    }

    private boolean isEligibleToBeUnreserved(SlashCommandEvent event, TextChannel channel, HelpConfig config, User owner) {
        return channelIsInReservedCategory(channel, config) &&
                (isUserWhoReservedChannel(event, owner) || memberHasHelperRole(event) || memberHasStaffRole(event));
    }

    private boolean channelIsInReservedCategory(TextChannel channel, HelpConfig config) {
        return config.getReservedChannelCategory().equals(channel.getParent());
    }

    private boolean isUserWhoReservedChannel(SlashCommandEvent event, User owner) {
        return event.getUser().equals(owner);
    }

    private boolean memberHasStaffRole(SlashCommandEvent event) {
        return event.getMember() != null &&
                event.getMember().getRoles().contains(Bot.config.get(event.getGuild()).getModeration().getStaffRole());
    }

    private boolean memberHasHelperRole(SlashCommandEvent event) {
        return event.getMember() != null &&
                event.getMember().getRoles().contains(Bot.config.get(event.getGuild()).getHelp().getHelperRole());
    }
}
