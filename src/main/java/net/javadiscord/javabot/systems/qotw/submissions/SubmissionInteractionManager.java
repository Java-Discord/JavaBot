package net.javadiscord.javabot.systems.qotw.submissions;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.ComponentHandler;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.util.Responses;

import java.util.List;

/**
 * Handles all interactions regarding the QOTW Submission System.
 */
public class SubmissionInteractionManager extends ComponentHandler {

	public SubmissionInteractionManager() {
		handleButtonIds("qotw-submission");
		handleSelectMenuIds("qotw-submission-select");
	}

	@Override
	public void handleButton(ButtonInteractionEvent event, Button button) {
		SubmissionManager manager = new SubmissionManager(Bot.config.get(event.getGuild()).getQotw());
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		switch (id[1]) {
			case "controls" -> SubmissionInteractionManager.handleControlButtons(id, event);
			case "submit" -> manager.handleSubmission(event, Integer.parseInt(id[2])).queue();
			case "delete" -> manager.handleThreadDeletion(event);
		}
	}

	@Override
	public void handleSelectMenu(SelectMenuInteractionEvent event, List<String> values) {
		event.deferReply(true).queue();
		String[] id = ComponentIdBuilder.split(event.getComponentId());
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
			case "decline" -> event.getMessage().editMessageComponents(ActionRow.of(buildDeclineMenu())).queue();
			case "delete" -> manager.deleteSubmission(event, thread);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	private static boolean hasPermissions(Member member) {
		QOTWConfig config = Bot.config.get(member.getGuild()).getQotw();
		return !member.getRoles().isEmpty() && member.getRoles().contains(config.getQOTWReviewRole());
	}

	private static SelectMenu buildDeclineMenu() {
		return SelectMenu.create("qotw-submission-select:decline")
				.setPlaceholder("Select a reason for declining this submission.")
				.setRequiredRange(1, 3)
				.addOption("Wrong Answer", "Wrong Answer", "The content of the submission was not correct.")
				.addOption("Incomplete Answer", "Incomplete Answer", "The submission was missing some important things and was overall incomplete.")
				.addOption("Too short", "Too short", "The submission was way too short in comparison to other submissions.")
				.build();
	}
}
