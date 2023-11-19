package net.discordjug.javabot.systems.qotw.submissions;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import xyz.dynxsty.dih4jda.interactions.components.StringSelectMenuHandler;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Handles all interactions regarding the QOTW Submission System.
 */
@AutoDetectableComponentHandler({"qotw-submission","qotw-submission-select"})
@RequiredArgsConstructor
public class SubmissionInteractionManager implements ButtonHandler, StringSelectMenuHandler {
	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final QuestionQueueRepository questionQueueRepository;
	private final ExecutorService asyncPool;

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, Button button) {
		SubmissionManager manager = new SubmissionManager(botConfig.get(event.getGuild()).getQotwConfig(), pointsService, questionQueueRepository, notificationService, asyncPool);
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		switch (id[1]) {
			case "submit" -> manager.handleSubmission(event, Integer.parseInt(id[2])).queue();
			case "delete" -> manager.handleThreadDeletion(event);
		}
	}

	@Override
	public void handleStringSelectMenu(@NotNull StringSelectInteractionEvent event, @NotNull List<String> values) {
		SubmissionManager manager = new SubmissionManager(botConfig.get(event.getGuild()).getQotwConfig(), pointsService, questionQueueRepository, notificationService, asyncPool);
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		switch (id[1]) {
			case "review" -> manager.handleSelectReview(event, id[2]);
		}
	}
}
