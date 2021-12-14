package com.javadiscord.javabot.service.help;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.data.h2db.DbActions;
import com.javadiscord.javabot.service.help.model.ChannelReservation;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

import java.sql.SQLException;

@Slf4j
public class HelpChannelInteractionManager {
    /**
     * Handles button interactions for help channel activity checks.
     *
     * @param event  The button event.
     * @param action The data extracted from the button id.
     */
    public void handleHelpChannel(ButtonClickEvent event, String reservationId, String action) {
        var config = Bot.config.get(event.getGuild()).getHelp();
        var channelManager = new HelpChannelManager(config);
        var optionalReservation = channelManager.getReservation(Long.parseLong(reservationId));
        if (optionalReservation.isEmpty()) {
            event.reply("Could not find reservation data for this channel. Perhaps it's no longer reserved?").setEphemeral(true).queue();
            event.getMessage().delete().queue();
            return;
        }
        var reservation = optionalReservation.get();
        TextChannel channel = event.getTextChannel();
        if (!event.isAcknowledged()) {
            event.deferReply(true).queue();
        }
        var owner = channelManager.getReservedChannelOwner(channel);
        // If a reserved channel doesn't have an owner, it's in an invalid state, but the system will handle it later automatically.
        if (owner == null) {
            // Remove the original message, just to make sure no more interactions are sent.
            event.getInteraction().getHook().sendMessage("Uh oh! It looks like this channel is no longer reserved, so these buttons can't be used.")
                    .setEphemeral(true).queue();
            event.getMessage().delete().queue();
            return;
        }

        if (owner.getIdLong() != reservation.getUserId() || channel.getIdLong() != reservation.getChannelId()) {
            event.getInteraction().getHook().sendMessage("The reservation data for this channel doesn't match up with Discord's information.")
                    .setEphemeral(true).queue();
            event.getMessage().delete().queue();
            return;
        }

        // Check that the user is allowed to do the interaction.
        if (
                event.getUser().equals(owner) ||
                        (event.getMember() != null && event.getMember().getRoles().contains(Bot.config.get(event.getGuild()).getModeration().getStaffRole()))
        ) {
            if (action.equals("done")) {
                event.getMessage().delete().queue();
                if (event.getUser().equals(owner)) {// If the owner is unreserving their own channel, handle it separately.
                    channelManager.unreserveChannelByUser(channel, owner, null, event);
                } else {
                    channelManager.unreserveChannel(channel);
                }
            } else if (action.equals("not-done")) {
                log.info("Removing timeout check message in {} because it was marked as not-done.", channel.getAsMention());
                event.getMessage().delete().queue();
                try {
                    int nextTimeout = channelManager.getNextTimeout(channel);
                    channelManager.setTimeout(channel, nextTimeout);
                    Responses.info(event.getHook(), null, String.format(
                            "Okay, we'll keep this channel reserved for you, and check again in **%d** minutes.",
                            nextTimeout
                    )).queue();
                } catch (SQLException e) {
                    Responses.error(event.getHook(), "An error occurred while managing this help channel.").queue();
                }
            }
        } else {
            Responses.error(event.getHook(), "Sorry, only the person who reserved this channel, or moderators, are allowed to use these buttons.")
                    .setEphemeral(true).queue();
        }
    }

    /**
     * Handles button interactions pertaining to the interaction provided to
     * users when they choose to unreserve their channel, giving them options to
     * thank helpers or cancel the unreserving.
     *
     * @param event  The button event.
     * @param action The data extracted from the button's id.
     */
    public void handleHelpThank(ButtonClickEvent event, String reservationId, String action) {
        var config = Bot.config.get(event.getGuild()).getHelp();
        var channelManager = new HelpChannelManager(config);
        var optionalReservation = channelManager.getReservation(Long.parseLong(reservationId));
        if (optionalReservation.isEmpty()) {
            event.getInteraction().getHook().sendMessage("Could not find reservation data for this channel. Perhaps it's no longer reserved?")
                    .setEphemeral(true).queue();
            event.getMessage().delete().queue();
            return;
        }
        var reservation = optionalReservation.get();
        TextChannel channel = event.getTextChannel();
        if (!event.isAcknowledged()) {
            event.deferReply(true).queue();
        }
        var owner = channelManager.getReservedChannelOwner(channel);
        if (owner == null) {
            event.getInteraction().getHook().sendMessage("Sorry, but this channel is currently unreserved.").setEphemeral(true).queue();
            event.getMessage().delete().queue();
            return;
        }

        if (owner.getIdLong() != reservation.getUserId() || channel.getIdLong() != reservation.getChannelId()) {
            event.getInteraction().getHook().sendMessage("The reservation data for this channel doesn't match up with Discord's information.")
                    .setEphemeral(true).queue();
            event.getMessage().delete().queue();
            return;
        }

        if (event.getUser().equals(owner)) {
            if (action.equals("done")) {
                event.getMessage().delete().queue();
                channelManager.unreserveChannel(channel).queue();
            } else if (action.equals("cancel")) {
                event.getInteraction().getHook().sendMessage("Unreserving of this channel has been cancelled.").setEphemeral(true).queue();
                event.getMessage().delete().queue();
                try {
                    channelManager.setTimeout(channel, config.getInactivityTimeouts().get(0));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                long helperId = Long.parseLong(action);
                thankHelper(event, channel, owner, helperId, reservation, channelManager);
            }
        } else {
            event.getInteraction().getHook().sendMessage("Sorry, only the person who reserved this channel can thank users.").setEphemeral(true).queue();
        }
    }

    private void thankHelper(ButtonClickEvent event, TextChannel channel, User owner, long helperId, ChannelReservation reservation, HelpChannelManager channelManager) {
        var btn = event.getButton();
        long thankCount = DbActions.count(
                "SELECT COUNT(id) FROM help_channel_thanks WHERE reservation_id = ? AND helper_id = ?",
                s -> {
                    s.setLong(1, reservation.getId());
                    s.setLong(2, helperId);
                }
        );
        if (thankCount > 0) {
            event.getInteraction().getHook().sendMessage("You can't thank someone twice!").setEphemeral(true).queue();
            if (btn != null) {
                event.editButton(btn.asDisabled()).queue();
            }
        } else {
            event.getJDA().retrieveUserById(helperId).queue(helper -> {
                // First insert the new thanks data.
                try {
                    DbActions.update(
                            "INSERT INTO help_channel_thanks (reservation_id, user_id, channel_id, helper_id) VALUES (?, ?, ?, ?)",
                            reservation.getId(),
                            owner.getIdLong(),
                            channel.getIdLong(),
                            helper.getIdLong()
                    );
                    event.getInteraction().getHook().sendMessageFormat("You thanked %s", helper.getAsTag()).setEphemeral(true).queue();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Bot.config.get(event.getGuild()).getModeration().getLogChannel().sendMessageFormat(
                            "Could not record user %s thanking %s for help in channel %s: %s",
                            owner.getAsTag(),
                            helper.getAsTag(),
                            channel.getAsMention(),
                            e.getMessage()
                    ).queue();
                }
                // Then disable the button, or unreserve the channel if there's nobody else to thank.
                if (btn != null) {
                    var activeButtons = event.getMessage().getButtons().stream()
                            .filter(b -> !b.isDisabled() && !b.getLabel().equals("Unreserve") && !b.getLabel().equals("Cancel") && !b.equals(btn))
                            .toList();
                    if (activeButtons.isEmpty()) {// If there are no more people to thank, automatically unreserve the channel.
                        event.getMessage().delete().queue();
                        channelManager.unreserveChannel(channel).queue();
                    } else {// Otherwise, disable the button.
                        event.editButton(btn.asDisabled()).queue();
                    }
                }
            });
        }
    }
}
