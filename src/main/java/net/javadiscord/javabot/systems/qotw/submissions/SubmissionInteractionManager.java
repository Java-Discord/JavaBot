package net.javadiscord.javabot.systems.qotw.submissions;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;

/**
 * Handles all interactions regarding the QOTW Submission System.
 */
public class SubmissionInteractionManager {

	private SubmissionInteractionManager() {}

	/**
	 * Handles all Interactions regarding the Submission System.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired.
	 * @param id The Buttons id, split by ":".
	 */
	public static void handleButton(ButtonInteractionEvent event, String[] id) {
		SubmissionManager manager = new SubmissionManager(Bot.config.get(event.getGuild()).getQotw());
		switch (id[1]) {
			case "controls" -> SubmissionInteractionManager.handleControlButtons(id, event);
			case "submit" -> manager.handleSubmission(event, Integer.parseInt(id[2])).queue();
			case "delete" -> manager.handleThreadDeletion(event);
		}
	}

	/**
	 * Handles Button interactions regarding the Submission Controls System.
	 *
	 * @param id    The button's id, split by ":".
	 * @param event The {@link ButtonInteractionEvent} that is fired upon use.
	 */
	public static void handleControlButtons(String[] id, ButtonInteractionEvent event) {
		event.deferReply(true).queue();
		SubmissionControlsManager manager = new SubmissionControlsManager(event.getGuild(), (ThreadChannel) event.getGuildChannel());
		if (!hasPermissions(event.getMember())) {
			event.getHook().sendMessage("Insufficient Permissions.").queue();
			return;
		}
		if (!event.getChannelType().isThread()) {
			event.getHook().sendMessage("This interaction may only be used in thread channels.").queue();
			return;
		}
		ThreadChannel thread = (ThreadChannel) event.getGuildChannel();
		switch (id[2]) {
			case "accept" -> manager.acceptSubmission(event, thread);
			case "decline" -> manager.declineButtonSubmission(event);
			case "delete" -> manager.deleteSubmission(event, thread);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	/**
	 * Handles Select Menu interactions regarding the Submission Controls System.
	 *
	 * @param id    The SelectionMenu's id.
	 * @param event The {@link SelectMenuInteractionEvent} that is fired upon use.
	 */
	public static void handleSelectMenu(String[] id, SelectMenuInteractionEvent event) {
		event.deferReply(true).queue();
		SubmissionControlsManager manager = new SubmissionControlsManager(event.getGuild(), (ThreadChannel) event.getGuildChannel());
		if (!hasPermissions(event.getMember())) {
			event.getHook().sendMessage("Insufficient Permissions.").setEphemeral(true).queue();
			return;
		}
		if (!event.getChannelType().isThread()) {
			event.getHook().sendMessage("This interaction may only be used in thread channels.").setEphemeral(true).queue();
			return;
		}
		ThreadChannel thread = (ThreadChannel) event.getGuildChannel();
		switch (id[1]) {
			case "decline" -> manager.declineSelectSubmission(event, thread);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	private static boolean hasPermissions(Member member) {
		QOTWConfig config = Bot.config.get(member.getGuild()).getQotw();
		return !member.getRoles().isEmpty() && member.getRoles().contains(config.getQOTWReviewRole());
	}
}
