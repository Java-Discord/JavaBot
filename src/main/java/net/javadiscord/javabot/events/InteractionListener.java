package net.javadiscord.javabot.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.help.HelpChannelInteractionManager;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionControlsManager;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionManager;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.staff.self_roles.SelfRoleInteractionManager;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * Listens for Interaction Events and handles them.
 */
@Slf4j
public class InteractionListener extends ListenerAdapter {

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		if (event.getUser().isBot()) return;
		String[] id = event.getModalId().split(":");
		switch (id[0]) {
			case "self-role" -> new SelfRoleInteractionManager().handleModalSubmit(event, id);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	@Override
	public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
		if (event.getUser().isBot()) return;
		String[] id = event.getComponentId().split(":");
		var config = Bot.config.get(event.getGuild());
		switch (id[0]) {
			case "submission-controls-select" -> {
				var thread = (ThreadChannel) event.getGuildChannel();
				try (var con = Bot.dataSource.getConnection()) {
					var repo = new QOTWSubmissionRepository(con);
					var submissionOptional = repo.getSubmissionByThreadId(thread.getIdLong());
					submissionOptional.ifPresent(qotwSubmission -> new SubmissionControlsManager(event.getGuild(), config.getQotw(), qotwSubmission).handleSelectMenus(id, event));
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		if (event.getUser().isBot()) return;
		String[] id = event.getComponentId().split(":");
		var config = Bot.config.get(event.getGuild());
		switch (id[0]) {
			case "qotw-submission" -> {
				var manager = new SubmissionManager(config.getQotw());
				if (!id[1].isEmpty() && id[1].equals("delete")) manager.handleThreadDeletion(event);
				else manager.handleSubmission(event, Integer.parseInt(id[1])).queue();
			}
			case "submission-controls" -> {
				var thread = (ThreadChannel) event.getGuildChannel();
				try (var con = Bot.dataSource.getConnection()) {
					var repo = new QOTWSubmissionRepository(con);
					var submissionOptional = repo.getSubmissionByThreadId(thread.getIdLong());
					submissionOptional.ifPresent(qotwSubmission -> new SubmissionControlsManager(event.getGuild(), config.getQotw(), qotwSubmission).handleButtons(id, event));
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			case "self-role" -> new SelfRoleInteractionManager().handleButton(event, id);
			case "help-channel" -> new HelpChannelInteractionManager().handleHelpChannel(event, id[1], id[2]);
			case "help-thank" -> new HelpChannelInteractionManager().handleHelpThank(event, id[1], id[2]);
			case "utils" -> this.handleUtils(id, event);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	/**
	 * Some utility methods for interactions.
	 * + May be useful for Context Menu Interactions.
	 *
	 * @param id The button's id, split by ":".
	 * @param event The {@link ButtonInteractionEvent} that is fired upon use.
	 */
	private void handleUtils(String[] id, ButtonInteractionEvent event) {
		event.deferEdit().queue();
		var service = new ModerationService(event.getInteraction());
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
}
