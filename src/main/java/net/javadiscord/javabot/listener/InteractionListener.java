package net.javadiscord.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.help.HelpChannelInteractionManager;
import net.javadiscord.javabot.systems.moderation.ReportCommand;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionControlsManager;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionManager;
import net.javadiscord.javabot.systems.staff.self_roles.SelfRoleInteractionManager;
import net.javadiscord.javabot.util.InteractionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for Interaction Events and handles them.
 */
@Slf4j
public class InteractionListener extends ListenerAdapter {

	@Override
	public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
		// TODO: add autocomplete interactions (next pr)
	}

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
			case "self-role" -> new SelfRoleInteractionManager().handleModalSubmit(event, id);
			case "report" -> new ReportCommand().handleModalSubmit(event, id);
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
			case "submission-controls-select" -> new SubmissionControlsManager(event.getGuild(), (ThreadChannel) event.getGuildChannel()).handleSelectMenus(id, event);
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
		var config = Bot.config.get(event.getGuild());
		switch (id[0]) {
			case "qotw-submission" -> {
				SubmissionManager manager = new SubmissionManager(config.getQotw());
				if (!id[1].isEmpty() && id[1].equals("delete")) {
					manager.handleThreadDeletion(event);
				} else {
					manager.handleSubmission(event, Integer.parseInt(id[1])).queue();
				}
			}
			case "submission-controls" -> new SubmissionControlsManager(event.getGuild(), (ThreadChannel) event.getGuildChannel()).handleButtons(id, event);
			case "resolve-report" -> new ReportCommand().markAsResolved(event, id[1]);
			case "self-role" -> new SelfRoleInteractionManager().handleButton(event, id);
			case "help-channel" -> new HelpChannelInteractionManager().handleHelpChannel(event, id[1], id[2]);
			case "help-thank" -> new HelpChannelInteractionManager().handleHelpThank(event, id[1], id[2]);
			case "utils" -> InteractionUtils.handleButton(event, id);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}
}
