package net.javadiscord.javabot.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.help.HelpChannelInteractionManager;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.qotw.SubmissionManager;

@Slf4j
public class InteractionListener extends ListenerAdapter {

	// TODO: add Context-Menu Commands (once they're available in JDA)
	// TODO: Clean up button ids. "qotw-submission" & "qotw-submission-delete" is just a temporary solution.
	@Override
	public void onButtonClick(ButtonClickEvent event) {
		if (event.getUser().isBot()) return;
		event.deferEdit().queue();
		String[] id = event.getComponentId().split(":");
		var config = Bot.config.get(event.getGuild());
		switch (id[0]) {
			case "qotw-submission" -> new SubmissionManager(config.getQotw()).handleSubmission(event, Long.parseLong(id[1])).queue();
			case "qotw-submission-delete" -> new SubmissionManager(config.getQotw()).handleThreadDeletion(event);
			case "reaction-role" -> this.handleReactionRoles(event);
			case "help-channel" -> new HelpChannelInteractionManager().handleHelpChannel(event, id[1], id[2]);
			case "help-thank" -> new HelpChannelInteractionManager().handleHelpThank(event, id[1], id[2]);
			case "utils" -> this.handleUtils(event);
		}
	}

	/**
	 * Some utility methods for interactions
	 * + May be useful for Context Menu Interactions
	 */
	private void handleUtils(ButtonClickEvent event) {
		var service = new ModerationService(event.getInteraction());
		String[] id = event.getComponentId().split(":");
		switch (id[1]) {
			case "delete" -> event.getHook().deleteOriginal().queue();
			case "kick" -> service.kick(
					event.getGuild().getMemberById(id[2]),
					"None",
					event.getMember(),
					event.getTextChannel(),
					false);
			case "ban" -> service.ban(
					event.getGuild().getMemberById(id[2]),
					"None",
					event.getMember(),
					event.getTextChannel(),
					false);
			case "unban" -> service.unban(
					Long.parseLong(id[2]),
					event.getMember(),
					event.getTextChannel(),
					false);
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
}
