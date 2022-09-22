package net.javadiscord.javabot.systems.qotw.submissions;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;
import com.dynxsty.dih4jda.interactions.components.SelectMenuHandler;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Handles all interactions regarding the QOTW Submission System.
 */
@AutoDetectableComponentHandler({"qotw-submission","qotw-submission-select"})
@RequiredArgsConstructor
public class SubmissionInteractionManager implements ButtonHandler, SelectMenuHandler {
	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final DbHelper dbHelper;
	private final QOTWSubmissionRepository qotwSubmissionRepository;
	private final QuestionQueueRepository questionQueueRepository;
	private final ExecutorService asyncPool;

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, Button button) {
		SubmissionManager manager = new SubmissionManager(botConfig.get(event.getGuild()).getQotwConfig(), dbHelper, qotwSubmissionRepository, questionQueueRepository);
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		switch (id[1]) {
			case "controls" -> handleControlButtons(id, event);
			case "submit" -> manager.handleSubmission(event, Integer.parseInt(id[2])).queue();
			case "delete" -> manager.handleThreadDeletion(event);
		}
	}

	@Override
	public void handleSelectMenu(@NotNull SelectMenuInteractionEvent event, List<String> values) {
		event.deferReply(true).queue();
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		SubmissionControlsManager manager = new SubmissionControlsManager(botConfig.get(event.getGuild()), (ThreadChannel) event.getGuildChannel(), pointsService, notificationService, asyncPool, qotwSubmissionRepository);
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
	public void handleControlButtons(String[] id, @NotNull ButtonInteractionEvent event) {
		event.deferReply(true).queue();
		SubmissionControlsManager manager = new SubmissionControlsManager(botConfig.get(event.getGuild()), (ThreadChannel) event.getGuildChannel(), pointsService, notificationService, asyncPool, qotwSubmissionRepository);
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

	private boolean hasPermissions(@NotNull Member member) {
		QOTWConfig config = botConfig.get(member.getGuild()).getQotwConfig();
		return !member.getRoles().isEmpty() && member.getRoles().contains(config.getQOTWReviewRole());
	}

	private static @NotNull SelectMenu buildDeclineMenu() {
		return SelectMenu.create("qotw-submission-select:decline")
				.setPlaceholder("Select a reason for declining this submission.")
				.setRequiredRange(1, 3)
				.addOption("Wrong Answer", "Wrong Answer", "The content of the submission was not correct.")
				.addOption("Incomplete Answer", "Incomplete Answer", "The submission was missing some important things and was overall incomplete.")
				.addOption("Too short", "Too short", "The submission was way too short in comparison to other submissions.")
				.build();
	}
}
