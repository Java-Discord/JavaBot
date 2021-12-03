package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.staff_commands.Ban;
import com.javadiscord.javabot.commands.staff_commands.Kick;
import com.javadiscord.javabot.commands.staff_commands.Unban;
import com.javadiscord.javabot.data.h2db.DbActions;
import com.javadiscord.javabot.service.help.HelpChannelManager;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.SQLException;

@Slf4j
public class InteractionListener extends ListenerAdapter {

	// TODO: add Context-Menu Commands (once they're available in JDA)

	@Override
	public void onButtonClick(ButtonClickEvent event) {
		if (event.getUser().isBot()) return;
		event.deferEdit().queue();

		String[] id = event.getComponentId().split(":");
		switch (id[0]) {
			case "dm-submission" -> this.handleDmSubmission(event);
			case "submission" -> this.handleSubmission(event);
			case "reaction-role" -> this.handleReactionRoles(event);
			case "help-channel" -> this.handleHelpChannel(event, id[1]);
			case "help-thank" -> this.handleHelpThank(event, id[1]);
			case "utils" -> this.handleUtils(event);
		}
	}

	/**
	 * Some utility methods for interactions
	 * + May be useful for Context Menu Interactions
	 */
	private void handleUtils(ButtonClickEvent event) {
		String[] id = event.getComponentId().split(":");
		switch (id[1]) {
			case "delete" -> event.getHook().deleteOriginal().queue();
			case "kick" -> new Kick().handleKickInteraction(event.getGuild().getMemberById(id[2]), event).queue();
			case "ban" -> new Ban().handleBanInteraction(event.getGuild().getMemberById(id[2]), event).queue();
			case "unban" -> new Unban().handleUnbanInteraction(event, id[2]).queue();
		}
	}

	private void handleDmSubmission(ButtonClickEvent event) {
		String[] id = event.getComponentId().split(":");
		switch (id[1]) {
			case "send" -> new SubmissionListener().dmSubmissionSend(event);
			case "cancel" -> new SubmissionListener().dmSubmissionCancel(event);
		}
	}

	private void handleSubmission(ButtonClickEvent event) {
		String[] id = event.getComponentId().split(":");
		switch (id[1]) {
			case "approve" -> new SubmissionListener().submissionApprove(event);
			case "decline" -> new SubmissionListener().submissionDecline(event);
			case "getraw" -> new SubmissionListener().submissionGetRaw(event);
		}
	}

	private void handleReactionRoles(ButtonClickEvent event) {
		String[] id = event.getComponentId().split(":");
		String roleID = id[1];
		boolean permanent = Boolean.parseBoolean(id[2]);

		event.getGuild().retrieveMemberById(event.getUser().getId()).queue(member->{
			Role role = event.getGuild().getRoleById(roleID);

			if (member.getRoles().contains(role)) {
				if (!permanent) {
					event.getGuild().removeRoleFromMember(member, role).queue();
					event.getHook().sendMessage("Removed Role: " + role.getAsMention()).setEphemeral(true).queue();
				} else {
					event.getHook().sendMessage("You already have Role: " + role.getAsMention()).setEphemeral(true).queue();
				}
			} else {
				event.getGuild().addRoleToMember(member, role).queue();
				event.getHook().sendMessage("Added Role: " + role.getAsMention()).setEphemeral(true).queue();
			}
		});
	}

	private void handleHelpChannel(ButtonClickEvent event, String action) {
		var config = Bot.config.get(event.getGuild()).getHelp();
		var channelManager = new HelpChannelManager(config);
		TextChannel channel = event.getTextChannel();
		User owner = channelManager.getReservedChannelOwner(channel);
		// If a reserved channel doesn't have an owner, it's in an invalid state, but the system will handle it later automatically.
		if (owner == null) {
			// Remove the original message, just to make sure no more interactions are sent.
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
				channelManager.unreserveChannelByUser(channel, owner, null, event);
			} else if (action.equals("not-done")) {
				log.info("Removing timeout check message in {} because it was marked as not-done.", channel.getAsMention());
				event.getMessage().delete().queue();
				try {
					int nextTimeout = channelManager.getNextTimeout(channel);
					channelManager.setTimeout(channel, nextTimeout);
					channel.sendMessage(String.format(
							"Okay, we'll keep this channel reserved for you, and check again in **%d** minutes.",
							nextTimeout
					)).queue();
				} catch (SQLException e) {
					Responses.error(event.getHook(), "An error occurred while managing this help channel.").queue();
				}
			}
		}
	}

	private void handleHelpThank(ButtonClickEvent event, String action) {
		var config = Bot.config.get(event.getGuild()).getHelp();
		var channelManager = new HelpChannelManager(config);
		TextChannel channel = event.getTextChannel();
		User owner = channelManager.getReservedChannelOwner(channel);
		if (owner == null) {
			event.getInteraction().getHook().sendMessage("Sorry, but this channel is currently unreserved.").setEphemeral(true).queue();
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
				event.getJDA().retrieveUserById(helperId).queue(user -> {
					event.getInteraction().getHook().sendMessageFormat("You thanked %s", user.getAsTag()).setEphemeral(true).queue();
					try {
						DbActions.update(
								"INSERT INTO help_channel_thanks (user_id, channel_id, helper_id) VALUES (?, ?, ?)",
								owner.getIdLong(),
								channel.getIdLong(),
								user.getIdLong()
						);
					} catch (SQLException e) {
						e.printStackTrace();
						Bot.config.get(event.getGuild()).getModeration().getLogChannel().sendMessageFormat(
								"Could not record user %s thanking %s for help in channel %s: %s",
								owner.getAsTag(),
								user.getAsTag(),
								channel.getAsMention(),
								e.getMessage()
						).queue();
					}
					var btn = event.getButton();
					if (btn != null) {
						var activeButtons = event.getMessage().getButtons().stream()
								.filter(b -> !b.isDisabled() && !b.getLabel().equals("Unreserve") && !b.getLabel().equals("Cancel") && !b.equals(btn))
								.toList();
						if (activeButtons.isEmpty()) {// If there are no more people to thank, automatically unreserve the channel.
							event.getMessage().delete().queue();
							channelManager.unreserveChannel(channel).queue();
						} else {// Otherwise,
							event.editButton(btn.asDisabled()).queue();
						}
					}
				});
			}
		} else {
			event.getInteraction().getHook().sendMessage("Sorry, only the person who reserved this channel can thank users.").setEphemeral(true).queue();
		}
	}
}
