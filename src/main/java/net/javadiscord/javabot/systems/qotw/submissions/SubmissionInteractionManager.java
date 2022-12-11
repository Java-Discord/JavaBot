package net.javadiscord.javabot.systems.qotw.submissions;

import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Handles all interactions regarding the QOTW Submission System.
 */
@AutoDetectableComponentHandler({"qotw-submission","qotw-submission-select"})
@RequiredArgsConstructor
public class SubmissionInteractionManager implements ButtonHandler {
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
}
