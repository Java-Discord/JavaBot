package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class checks all incoming messages for potential spam/advertising and warns or mutes the potential offender.
 */
@Slf4j
// TODO: Refactor this to be more efficient. Especially AutoMod#checkNewMessageAutomod
public class AutoMod extends ListenerAdapter {

	private static final Pattern INVITE_URL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}/)\\w+");
	private static final Pattern URL_PATTERN = Pattern.compile(
			"(?:^|[\\W])((ht|f)tp(s?)://|www\\.)"
					+ "(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*"
					+ "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private List<String> spamUrls;

	/**
	 * Constructor of the class, that creates a list of strings with potential spam/scam urls.
	 */
	public AutoMod() {
		try(Scanner scan = new Scanner(new URL("https://raw.githubusercontent.com/DevSpen/scam-links/master/src/links.txt").openStream()).useDelimiter("\\A")) {
			String response = scan.next();
			spamUrls = List.of(response.split("\n"));
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
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
		if (message.getMentions().getUsers().size() >= 5) {
			new ModerationService(Bot.config.get(message.getGuild()))
					.warn(
							message.getAuthor(),
							WarnSeverity.MEDIUM,
							"Automod: Mention Spam",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getChannel(),
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
		//Check for Advertising Links
		if (hasAdvertisingLink(message)) {
			new GuildNotificationService(message.getGuild()).sendLogChannelNotification("Message: `" + message.getContentRaw() + "`");
			new ModerationService(Bot.config.get(message.getGuild()))
					.warn(
							message.getAuthor(),
							WarnSeverity.MEDIUM,
							"Automod: Advertising",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getChannel(),
							isSuggestionsChannel(message.getChannel().asTextChannel())
					);
			message.delete().queue(success -> {
			}, error -> log.info("Message was deleted before Automod was able to handle it."));


		}

		//Check for suspicious Links
		if (hasSuspiciousLink(message)) {
			new GuildNotificationService(message.getGuild()).sendLogChannelNotification("Suspicious Link sent by: %s (`%s`)", message.getMember().getAsMention(), message);
			new ModerationService(Bot.config.get(message.getGuild()))
					.warn(
							message.getAuthor(),
							WarnSeverity.MEDIUM,
							"Automod: Suspicious Link",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getChannel(),
							isSuggestionsChannel(message.getChannel().asTextChannel())
					);
			message.delete().queue(success -> {
			}, error -> log.info("Message was deleted before Automod was able to handle it."));
		}
	}

	/**
	 * Handles potential spam messages.
	 *
	 * @param msg    the message
	 * @param member the member to be potentially warned
	 */
	private void handleSpam(@Nonnull Message msg, Member member) {
		// java files -> not spam
		if (!msg.getAttachments().isEmpty() && msg.getAttachments().stream().allMatch(a -> Objects.equals(a.getFileExtension(), "java"))) {
			return;
		}
		new ModerationService(Bot.config.get(member.getGuild()))
				.timeout(
						member,
						"Automod: Spam",
						msg.getGuild().getSelfMember(),
						Duration.of(6, ChronoUnit.HOURS),
						msg.getChannel(),
						false
				);
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
		final String messageRaw = message.getContentRaw();
		Matcher urlMatcher = URL_PATTERN.matcher(messageRaw);
		if (messageRaw.contains("http://") || messageRaw.contains("https://")) {
			// only do it for a links, so it won't iterate for each message
			while (urlMatcher.find()) {
				String url = urlMatcher.group(0).trim();
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
		Matcher matcher = INVITE_URL.matcher(cleanString(message.getContentRaw()));
		if (matcher.find()) {
			return Bot.config.get(message.getGuild()).getModerationConfig().getAutomodInviteExcludes().stream().noneMatch(message.getContentRaw()::contains);
		}
		return false;
	}

	private boolean isSuggestionsChannel(@NotNull TextChannel channel) {
		return channel.equals(Bot.config.get(channel.getGuild()).getModerationConfig().getSuggestionChannel());
	}
}