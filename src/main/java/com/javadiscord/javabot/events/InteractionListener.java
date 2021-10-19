package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.moderation.Ban;
import com.javadiscord.javabot.commands.moderation.Kick;
import com.javadiscord.javabot.service.help.HelpChannelManager;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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
			case "utils" -> this.handleUtils(event);
		}
	}

	/**
	 * Some utility methods for interactions
	 */
	private void handleUtils(ButtonClickEvent event) {
		String[] id = event.getComponentId().split(":");
		switch (id[1]) {
			case "delete" -> event.getHook().deleteOriginal().queue();
			case "kick" -> new Kick().handleKickInteraction(event.getGuild().getMemberById(id[2]), event);
			case "ban" -> new Ban().handleBanInteraction(event.getGuild().getMemberById(id[2]), event);
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

		Member member = event.getGuild().retrieveMemberById(event.getUser().getId()).complete();
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
				log.info("Unreserving channel {} because it was marked as done.", channel.getAsMention());
				event.getMessage().delete().queue();
				channelManager.unreserveChannel(channel).queue();
			} else if (action.equals("not-done")) {
				log.info("Removing timeout check message in {} because it was marked as not-done.", channel.getAsMention());
				event.getMessage().delete().queue();
				channel.sendMessage(String.format(
						"Okay, we'll keep this channel reserved for you, and check again in **%d** minutes.",
						config.getInactivityTimeoutMinutes()
				)).queue();
			}
		}
	}
}
