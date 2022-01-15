package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SubmissionManager {
	private final QOTWConfig config;

	public WebhookMessageAction<?> handleSubmission(ButtonClickEvent event, long questionNumber) {
		if (!isLatestQOTWMessage(event.getMessage())) {
			return Responses.error(event.getHook(), "You may only answer to the newest QOTW.");
		}
		var member = event.getMember();
		if (hasActiveSubmissionThread(member, questionNumber)) {
			var thread = getSubmissionThread(member, questionNumber);
			return Responses.error(event.getHook(), "You already have a submission thread: " + thread.get().getAsMention());
		}
		if (!canCreateSubmissions(event.getMember(), questionNumber)) {
			return Responses.warning(event.getHook(), "You're not eligible to create a new submission thread.");
		}
		config.getSubmissionChannel()
				.createThreadChannel(String.format("Submission by %s | %s (%s)",
								member.getEffectiveName(), member.getId(), questionNumber),
						true).queue(
						thread -> {
							var manager = thread.getManager();
							manager.setInvitable(false).queue();
							manager.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK).queue();
							thread.sendMessageFormat(
									"""
											**Question of the Week #%s**

											Hey, %s! Please submit your answer into this thread.
											You can even send multiple messages, if you want to. This whole thread counts as your submission.
											The %s will review your submission once a new question appears.""",
									questionNumber, member.getAsMention(), config.getQOTWReviewRole().getAsMention()).queue();
						}, e -> log.error("Could not create submission thread for member {}. ", member.getUser().getAsTag(), e)
				);
		log.info("Opened new Submission Thread for User {}", member.getUser().getAsTag());
		return Responses.success(event.getHook(),
				"Submission Thread created",
				"Successfully created a new private Thread for your submission.");
	}

	private boolean isLatestQOTWMessage(Message message) {
		Message latestMessage = null;
		var history = message.getChannel().getHistory();
		while (latestMessage == null) {
			var messages = history.retrievePast(100).complete();
			for (var m : messages) {
				if (m.getAuthor().getIdLong() == m.getJDA().getSelfUser().getIdLong()) {
					latestMessage = m;
					break;
				}
			}
		}
		return message.equals(latestMessage);
	}

	private boolean canCreateSubmissions(Member member, long questionNumber) {
		if (member == null) return false;
		if (member.getUser().isBot() || member.getUser().isSystem()) return false;
		if (member.isTimedOut() || member.isPending()) return false;
		return !hasActiveSubmissionThread(member, questionNumber);
	}

	public boolean hasActiveSubmissionThread(Member member, long questionNumber) {
		var optional = getSubmissionThread(member, questionNumber);
		return optional.isPresent() && !optional.get().isArchived();
	}

	public Optional<ThreadMember> getSubmissionThreadOwner(ThreadChannel channel) {
		return channel.getThreadMembers().stream().filter(m -> channel.getName().contains(m.getId())).findFirst();
	}

	public Optional<ThreadChannel> getSubmissionThread(Member member, long questionNumber) {
		return config.getSubmissionChannel().getThreadChannels()
				.stream()
				.filter(s -> s.getName().contains(String.format("| %s (%s)", member.getId(), questionNumber)) && !s.isArchived())
				.findFirst();
	}

	public String archiveThreadContents(ThreadChannel channel) {
		var history = channel.getHistory();
		var messageCount = channel.getMessageCount();
		StringBuilder sb = new StringBuilder();
		while (messageCount > 0) {
			var messages = history.retrievePast(100).complete();
			for (var message : messages) {
				if (message.getAuthor() == message.getJDA().getSelfUser()) continue;
				sb.insert(0, message.getContentRaw() + "\n\n");
			}
			messageCount -= 100;
		}
		return sb.toString();
	}
}
