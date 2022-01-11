package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;
import net.javadiscord.javabot.util.StringResourceCache;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class checks all incoming messages for potential spam/advertising and warns or mutes the potential offender.
 */
@Slf4j
public class AutoMod extends ListenerAdapter {

	private static final Pattern inviteURL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}/)\\w+");
	private List<String> spamUrls;

	public AutoMod() {
		try {
			spamUrls = Arrays.stream(StringResourceCache.load("/spamLinks.txt").split(System.lineSeparator())).toList();
		} catch (Exception e) {
			e.printStackTrace();
			spamUrls = List.of();
		}
		log.info("Loaded {} spam URLs!", spamUrls.size());
	}

	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
		Member member = event.getMember();
		if (canBypassAutomod(member)) return;
		checkNewMessageAutomod(event.getMessage());
	}

	@Override
	public void onMessageUpdate(@Nonnull MessageUpdateEvent event) {
		Member member = event.getMember();
		if (canBypassAutomod(member)) return;
		checkContentAutomod(event.getMessage());
	}

	/**
	 * Checks if a member can bypass the automod system.
	 *
	 * @param member the {@link Member} to check
	 * @return <code>true</code> if the member is allowed to bypass automod, else <code>false</code>
	 */
	private boolean canBypassAutomod(Member member) {
		return member == null
				|| member.getUser().isBot()
				|| member.hasPermission(Permission.MESSAGE_MANAGE);
	}

	/**
	 * Runs all automod checks that should be run when a message is sent.
	 *
	 * @param message the {@link Message} that should be checked
	 */
	private void checkNewMessageAutomod(@Nonnull Message message) {
		// mention spam
		if (message.getMentionedMembers().size() >= 5) {
			new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()).getModeration())
					.warn(
							message.getMember(),
							WarnSeverity.MEDIUM,
							"Automod: Mention Spam",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getTextChannel(),
							false
					);
		}

		// spam
		message.getChannel().getHistory().retrievePast(10).queue(messages -> {
			int spamCount = (int) messages.stream().filter(msg -> !msg.equals(message))
					// filter for spam
					.filter(msg -> msg.getAuthor().equals(message.getAuthor()) && !msg.getAuthor().isBot())
					.filter(msg -> (message.getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6).count();
			if (spamCount > 5) {
				handleSpam(message, message.getMember());
			}
		});

		checkContentAutomod(message);
	}

	/**
	 * Runs all automod checks only depend on the message content.
	 *
	 * @param message the {@link Message} that should be checked
	 */
	private void checkContentAutomod(@Nonnull Message message) {
		// Advertising
		Matcher matcher = inviteURL.matcher(cleanString(message.getContentRaw()));
		if (matcher.find()) {
			new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()).getModeration())
					.warn(
							message.getMember(),
							WarnSeverity.LOW,
							"Automod: Advertising",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getTextChannel(),
							false
					);
			message.delete().queue();
		}
		final String messageRaw = message.getContentRaw();
		if (messageRaw.contains("http://") || messageRaw.contains("https://")) {
			// only do it for a links, so it won't iterate for each message
			for (String spamUrl : spamUrls) {
				if (messageRaw.contains(spamUrl)) {
					try {
						new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()).getModeration())
								.warn(
										message.getMember(),
										WarnSeverity.HIGH,
										"Automod: Suspicious Link",
										message.getGuild().getMember(message.getJDA().getSelfUser()),
										message.getTextChannel(),
										false
								);
						message.delete().queue();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Handles potential spam messages
	 *
	 * @param msg    the message
	 * @param member the member to be potentially warned
	 */
	private void handleSpam(@Nonnull Message msg, Member member) {
		// java files -> not spam
		if (!msg.getAttachments().isEmpty()
				&& "java".equals(msg.getAttachments().get(0).getFileExtension())) return;

		new ModerationService(member.getJDA(), Bot.config.get(member.getGuild()).getModeration())
				.timeout(
						member,
						"Automod: Spam",
						msg.getGuild().getSelfMember(),
						Duration.of(6, ChronoUnit.HOURS),
						msg.getTextChannel(),
						false
				);
	}

	/**
	 * returns the original String cleaned up of unused code points and spaces
	 *
	 * @param input the input String
	 * @return the cleaned-up String
	 */
	private String cleanString(String input) {
		input = input.replaceAll("\\p{C}", "");
		input = input.replace(" ", "");
		return input;
	}
}

