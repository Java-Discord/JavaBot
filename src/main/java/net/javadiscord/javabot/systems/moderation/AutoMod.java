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
import net.javadiscord.javabot.util.GuildUtils;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class checks all incoming messages for potential spam/advertising and warns or mutes the potential offender.
 */
@Slf4j
public class AutoMod extends ListenerAdapter {

	private final Pattern INVITE_URL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}/)\\w+");
	private final Pattern URL_PATTERN = Pattern.compile(
			"(?:^|[\\W])((ht|f)tp(s?)://|www\\.)"
					+ "(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*"
					+ "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private List<String> spamUrls;

	/**
	 * Constructor of the class, that creates a list of strings with potential spam/scam urls.
	 */
	public AutoMod() {
		try {
			URL url = new URL("https://raw.githubusercontent.com/DevSpen/scam-links/master/src/links.txt");
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			InputStream stream = connection.getInputStream();
			String response = new Scanner(stream).useDelimiter("\\A").next();
			spamUrls = List.of(response.split("\n"));
		} catch (IOException e) {
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
			new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()))
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
		//Check for Advertising Links
		if (hasAdvertisingLink(message)) {
			GuildUtils.getLogChannel(message.getGuild()).sendMessage("Message: `" + message.getContentRaw() + "`").queue();
			new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()))
					.warn(
							message.getMember(),
							WarnSeverity.MEDIUM,
							"Automod: Advertising",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getTextChannel(),
							isSuggestionsChannel(message.getTextChannel())
					);
			message.delete().queue(success -> {
			}, error -> log.info("Message was deleted before Automod was able to handle it."));


		}

		//Check for suspicious Links
		if (hasSuspiciousLink(message)) {
			GuildUtils.getLogChannel(message.getGuild()).sendMessage(String.format("Suspicious Link sent by: %s (`%s`)", message.getMember().getAsMention(), message)).queue();
			new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()))
					.warn(
							message.getMember(),
							WarnSeverity.MEDIUM,
							"Automod: Suspicious Link",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getTextChannel(),
							isSuggestionsChannel(message.getTextChannel())
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
		if (!msg.getAttachments().isEmpty() && msg.getAttachments().stream().allMatch(a -> a.getFileExtension().equals("java"))) {
			return;
		}
		new ModerationService(member.getJDA(), Bot.config.get(member.getGuild()))
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
	 * returns the original String cleaned up of unused code points and spaces.
	 *
	 * @param input the input String.
	 * @return the cleaned-up String.
	 */
	private String cleanString(String input) {
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
	public boolean hasSuspiciousLink(Message message) {
		final String messageRaw = message.getContentRaw();
		Matcher urlMatcher = URL_PATTERN.matcher(messageRaw);
		if (messageRaw.contains("http://") || messageRaw.contains("https://")) {
			// only do it for a links, so it won't iterate for each message
			while (urlMatcher.find()) {
				String url = urlMatcher.group(0).trim();
				try {
					URI uri = new URI(url);
					if (spamUrls.contains(uri.getHost())) {
						return true;
					}
				} catch (URISyntaxException e) {
					e.printStackTrace();
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
	public boolean hasAdvertisingLink(Message message) {
		// Advertising
		Matcher matcher = INVITE_URL.matcher(cleanString(message.getContentRaw()));
		if (matcher.find()) {
			return Arrays.stream(Bot.config.get(message.getGuild()).getModeration().getAutomodInviteExcludes()).noneMatch(message.getContentRaw()::contains);
		}
		return false;
	}

	private boolean isSuggestionsChannel(TextChannel channel) {
		return channel.equals(Bot.config.get(channel.getGuild()).getModeration().getSuggestionChannel());
	}
}