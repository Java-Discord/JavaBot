package net.discordjug.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.h2db.message_cache.MessageCache;
import net.discordjug.javabot.systems.moderation.warn.model.WarnSeverity;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.MessageUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class checks all incoming messages for potential spam/advertising and warns or mutes the potential offender.
 */
@Slf4j
public class AutoMod extends ListenerAdapter {

	private static final Pattern INVITE_URL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)/.{0,4}|(?:app)?\\.com.{1,4}(?:invite|oauth2).{0,5}/)\\w+");
	private static final Pattern URL_PATTERN = Pattern.compile(
			"(?:^|[\\W])((ht|f)tp(s?)://|www\\.)"
					+ "(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*"
					+ "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private List<String> spamUrls;
	private final ModerationService moderationService;
	private final MessageCache messageCache;

	/**
	 * Constructor of the class, that creates a list of strings with potential spam/scam urls.
	 * @param notificationService The {@link QOTWPointsService}
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 * @param messageCache service for retrieving cached messages
	 */
	public AutoMod(NotificationService notificationService, BotConfig botConfig, ModerationService moderationService, MessageCache messageCache) {
		this.notificationService = notificationService;
		this.botConfig = botConfig;
		this.moderationService = moderationService;
		this.messageCache = messageCache;
		try(Scanner scan = new Scanner(new URL("https://raw.githubusercontent.com/DevSpen/scam-links/master/src/links.txt").openStream()).useDelimiter("\\A")) {
			String response = scan.next();
			spamUrls = List.of(response.split("\n"));
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			spamUrls = Collections.emptyList();
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
		// spam
		long spamCount = messageCache.getMessagesAfter(message.getTimeCreated().minusSeconds(6))
			.stream()
			.filter(cached -> cached.getMessageId() != message.getIdLong()) // exclude new/current message
			.filter(cached -> cached.getAuthorId() == message.getAuthor().getIdLong())
			.filter(cached -> 
				// only java files -> not spam
				cached.getAttachments().isEmpty() || 
				cached.getAttachments().stream()
					.anyMatch(attachment -> !attachment.contains(".java?")))
			.count() + 1; // include new message
			
		if (spamCount >= 5) {
			handleSpam(message, message.getMember());
		}

		checkContentAutomod(message);
	}

	/**
	 * Runs all automod checks only depend on the message content.
	 *
	 * @param message the {@link Message} that should be checked
	 */
	private void checkContentAutomod(@Nonnull Message message) {
		//Check for Advertising Links
		if (hasAdvertisingLink(message)) {
			doAutomodActions(message,"Advertising");
		}

		//Check for suspicious Links
		if (hasSuspiciousLink(message)) {
			doAutomodActions(message, "Suspicious Link");
		}
	}

	private void doAutomodActions(Message message, String reason) {
		notificationService.withGuild(message.getGuild()).sendToModerationLog(c -> c.sendMessageFormat("Message by %s: `%s`", message.getAuthor().getAsMention(), MessageUtils.getMessageContent(message)));
		moderationService
				.warn(
						message.getAuthor(),
						WarnSeverity.MEDIUM,
						"Automod: " + reason,
						message.getGuild().getMember(message.getJDA().getSelfUser()),
						message.getChannel(),
						isSuggestionsChannel(message.getChannel())
				);
		message.delete().queue(success -> {
		}, error -> log.info("Message was deleted before Automod was able to handle it."));
		message.getMember().timeoutFor(30, TimeUnit.SECONDS).queue();
	}

	/**
	 * Handles detected spam messages.
	 *
	 * @param msg    the (last) spam message
	 * @param member the member to be potentially warned
	 */
	private void handleSpam(@Nonnull Message msg, Member member) {
		moderationService
				.timeout(
						member.getUser(),
						"Automod: Spam",
						msg.getGuild().getSelfMember(),
						Duration.of(6, ChronoUnit.HOURS),
						msg.getChannel(),
						false
				);
		msg.delete().queue();
	}

	/**
	 * returns the original String cleaned up of unused code points and spaces.
	 *
	 * @param input the input String.
	 * @return the cleaned-up String.
	 */
	private @NotNull String cleanString(String input) {
		input = input.replaceAll("\\p{C}", "");
		input = input.replace(" ", "");
		return input;
	}

	/**
	 * Checks whether the given message contains a link that might be used to scam people.
	 *
	 * @param message The message to check.
	 * @return True if a link is found and False if not.
	 */
	public boolean hasSuspiciousLink(@NotNull Message message) {
		final String messageRaw = MessageUtils.getMessageContent(message);
		Matcher urlMatcher = URL_PATTERN.matcher(messageRaw);
		if (messageRaw.contains("http://") || messageRaw.contains("https://")) {
			// only do it for a links, so it won't iterate for each message
			while (urlMatcher.find()) {
				String url = urlMatcher.group(0).trim();
				if (url.startsWith("http://") || url.startsWith("https://")) {
					try {
						URI uri = new URI(url);
						if (uri.getHost() != null && spamUrls.contains(uri.getHost())) {
							return true;
						}
					} catch (URISyntaxException e) {
						ExceptionLogger.capture(e, getClass().getSimpleName());
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks whether the given message contains a discord invite link.
	 *
	 * @param message The Message to check.
	 * @return True if an invite is found and False if not.
	 */
	public boolean hasAdvertisingLink(@NotNull Message message) {
		// Advertising
		Matcher matcher = INVITE_URL.matcher(cleanString(MessageUtils.getMessageContent(message)));
		int start = 0;
		while (matcher.find(start)) {
			if (botConfig.get(message.getGuild()).getModerationConfig().getAutomodInviteExcludes().stream().noneMatch(matcher.group()::contains)) {
				return true;
			}
			start = matcher.start() + 1;
		}
		return false;
	}

	private boolean isSuggestionsChannel(@NotNull MessageChannelUnion channel) {
		return channel.getType().isGuild() &&
				channel.getIdLong() == botConfig.get(channel.asGuildMessageChannel().getGuild()).getModerationConfig().getSuggestionChannel().getIdLong();
	}
	
}