package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SubmissionManager {
	private final QOTWConfig config;

	public WebhookMessageAction<?> handleSubmission(ButtonClickEvent event, long questionNumber) {
		if (!isLatestQOTWMessage(event.getMessage())) {
			return Responses.error(event.getHook(), "You may only answer the newest QOTW.");
		}
		var member = event.getMember();
		if (hasActiveSubmissionThreads(member)) {
			var thread = getSubmissionThreads(member);
			return Responses.error(event.getHook(), "You already have a submission thread: " + thread.get(0).getAsMention());
		}
		if (!canCreateSubmissions(event.getMember())) {
			return Responses.warning(event.getHook(), "You're not eligible to create a new submission thread.");
		}
		config.getSubmissionChannel().createThreadChannel(
				String.format("Submission by %s | %s (%s)", member.getEffectiveName(), member.getId(), questionNumber), true).queue(
				thread -> {
					var manager = thread.getManager();
					manager.setInvitable(false).queue();
					manager.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK).queue();
					thread.sendMessageFormat("**Question of the Week #%s**\n" +
											"\nHey, %s! Please submit your answer into this thread." +
											"\nYou can even send multiple messages, if you want to. This whole thread counts as your submission." +
											"\nThe %s will review your submission once a new question appears.",
									questionNumber, member.getAsMention(), config.getQOTWReviewRole().getAsMention())
							.queue();
				}, e -> log.error("Could not create submission thread for member {}. ", member.getUser().getAsTag(), e)
		);
		log.info("Opened new Submission Thread for User {}", member.getUser().getAsTag());
		return Responses.success(event.getHook(), "Submission Thread created",
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

	private boolean canCreateSubmissions(Member member) {
		if (member == null) return false;
		if (member.getUser().isBot() || member.getUser().isSystem()) return false;
		if (member.isTimedOut() || member.isPending()) return false;
		return !hasActiveSubmissionThreads(member);
	}

	public boolean hasActiveSubmissionThreads(Member member) {
		return getSubmissionThreads(member).size() > 0;
	}

	public Member getSubmissionThreadOwner(ThreadChannel channel) {
		var message = channel.getHistoryFromBeginning(50)
				.complete().retrieveFuture(50).complete()
				.stream().filter(m -> m.getAuthor().equals(channel.getJDA().getSelfUser()))
				.limit(1).findFirst();
		return message.map(value -> value.getMentionedMembers().get(0)).orElse(null);
	}

	public List<ThreadChannel> getSubmissionThreads(Member member) {
		return config.getSubmissionChannel().getThreadChannels()
				.stream()
				.filter(c -> getSubmissionThreadOwner(c).equals(member) && !c.isArchived())
				.toList();
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
