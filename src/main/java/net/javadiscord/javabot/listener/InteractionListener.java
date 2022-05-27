package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.systems.help.HelpChannelInteractionManager;
import net.javadiscord.javabot.systems.commands.subcommands.leaderboard.ExperienceLeaderboardSubcommand;
import net.javadiscord.javabot.systems.moderation.ReportCommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionInteractionManager;
import net.javadiscord.javabot.systems.staff.self_roles.SelfRoleInteractionManager;
import net.javadiscord.javabot.util.InteractionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for Interaction Events and handles them.
 */
public class InteractionListener extends ListenerAdapter {

	/**
	 * Handles the {@link ModalInteractionEvent} and executes the corresponding interaction, based on the id.
	 *
	 * @param event The {@link ModalInteractionEvent} that was fired.
	 */
	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		if (event.getUser().isBot()) return;
		String[] id = event.getModalId().split(":");
		switch (id[0]) {
			case "self-role" -> SelfRoleInteractionManager.handleModalSubmit(event, id);
			case "report" -> new ReportCommand().handleModalSubmit(event, id);
			case "qotw-add-question" -> AddQuestionSubcommand.handleModalSubmit(event).queue();
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	/**
	 * Handles the {@link SelectMenuInteractionEvent} and executes the corresponding interaction, based on the id.
	 *
	 * @param event The {@link SelectMenuInteractionEvent} that was fired.
	 */
	@Override
	public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
		if (event.getUser().isBot()) return;
		String[] id = event.getComponentId().split(":");
		switch (id[0]) {
			case "qotw-submission-select" -> SubmissionInteractionManager.handleSelectMenu(id, event);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	/**
	 * Handles the {@link ButtonInteractionEvent} and executes the corresponding interaction, based on the id.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired.
	 */
	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		if (event.getUser().isBot()) return;
		String[] id = event.getComponentId().split(":");
		switch (id[0]) {
			case "experience-leaderboard" -> ExperienceLeaderboardSubcommand.handleButtons(event, id);
			case "qotw-submission" -> SubmissionInteractionManager.handleButton(event, id);
			case "resolve-report" -> new ReportCommand().markAsResolved(event, id[1]);
			case "self-role" -> SelfRoleInteractionManager.handleButton(event, id);
			case "help-channel" -> new HelpChannelInteractionManager().handleHelpChannel(event, id[1], id[2]);
			case "help-thank" -> new HelpChannelInteractionManager().handleHelpThank(event, id[1], id[2]);
			case "utils" -> InteractionUtils.handleButton(event, id);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}
}
