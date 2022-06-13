package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.commands.IdCalculatorCommand;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
		try(Scanner scan = new Scanner(new URL("https://raw.githubusercontent.com/DevSpen/scam-links/master/src/links.txt").openStream()).useDelimiter("\\A")) {
			String response = scan.next();
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
		AtomicBoolean consideredSpam = new AtomicBoolean(false);

		// mention spam
		if (message.getMentions().getUsersBag().size() >= 5) {
			new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()))
					.warn(
							message.getMember(),
							WarnSeverity.MEDIUM,
							"Automod: Mention Spam",
							message.getGuild().getMember(message.getJDA().getSelfUser()),
							message.getTextChannel(),
							false
					);
			consideredSpam.set(true);
		}

		// spam
		message.getChannel().getHistory().retrievePast(10).queue(messages -> {
			int spamCount = (int) messages.stream().filter(msg -> !msg.equals(message))
					// filter for spam
					.filter(msg -> msg.getAuthor().equals(message.getAuthor()) && !msg.getAuthor().isBot())
					.filter(msg -> (message.getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6).count();
			if (spamCount > 5) {
				handleSpam(message, message.getMember());
				consideredSpam.set(true);
			}
		});

		// check if the user has the required membership time to send media
		if (Bot.config.get(message.getGuild()).getModeration().getMinActiveHoursToSendMedia() > 0 && message.getMember() != null) {
			List<String> imageOrVideo = new ArrayList<>();
			for (Attachment attachment : message.getAttachments()) {
				if (attachment.isImage() || attachment.isVideo()) {
					imageOrVideo.add(attachment.getUrl());
				}
			}

			Duration duration = Duration.between(message.getMember().getTimeJoined(), message.getTimeCreated());
			long minHours = Bot.config.get(message.getGuild()).getModeration().getMinActiveHoursToSendMedia();

			if (duration.toHours() < minHours) {
				if (message.getType().canDelete()) {
					message.delete().queue();
				}

				if(!consideredSpam.get()) {
					message.getChannel()
							.sendMessage(message.getMember().getAsMention())
							.setEmbeds(buildNotEnoughTimeEmbed(message, minHours, TimeUnit.HOURS))
							.queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS));
				}

				Bot.config.get(message.getGuild()).getMessageCache().getMessageCacheLogChannel()
						.sendMessageEmbeds(this.buildNotEnoughTimeLogEmbed(message, imageOrVideo)).queue();
			}
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
			new GuildNotificationService(message.getGuild()).sendLogChannelNotification("Message: `" + message.getContentRaw() + "`");
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
			new GuildNotificationService(message.getGuild()).sendLogChannelNotification("Suspicious Link sent by: %s (`%s`)", message.getMember().getAsMention(), message);
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
					if (uri.getHost() != null && spamUrls.contains(uri.getHost())) {
						return true;
					}
				} catch (URISyntaxException e) {
					log.error("Error while parsing URL: " + url, e);
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

	/**
	 * Builds an embed that tells the user that he/she has to wait a certain amount of time before posting media.
	 * @param message The message that was sent.
	 * @param delay The delay that the user has to wait.
	 * @param delayUnit The unit of the delay.
	 * @return The {@link MessageEmbed}.
	 */
	public MessageEmbed buildNotEnoughTimeEmbed(Message message, long delay, TimeUnit delayUnit) {
		return new EmbedBuilder()
				.setAuthor("Auto Mod", null, message.getJDA().getSelfUser().getEffectiveAvatarUrl())
				.setTitle("Not Enough Time on Server")
				.setColor(Bot.config.get(message.getGuild()).getSlashCommand().getErrorColor())
				.setDescription(String.format(
						"You have to be on this server for at least `%d %s` before you can post media.",
								delay, delayUnit.toString().toLowerCase()))
				.setTimestamp(Instant.now())
				.setFooter(message.getAuthor().getAsTag(), message.getAuthor().getEffectiveAvatarUrl())
				.build();
	}

	/**
	 * Builds an embed that tells the moderators that a user tried to post media while he is not active long enough.
	 *
	 * @param message The message that was sent.
	 * @param links A {@link List} of attachment links that were sent.
	 * @return The {@link MessageEmbed}.
	 */
	private MessageEmbed buildNotEnoughTimeLogEmbed(Message message, List<String> links) {
		long epoch = IdCalculatorCommand.getTimestampFromId(message.getIdLong()) / 1000;
		return new EmbedBuilder()
				.setAuthor(message.getAuthor().getAsTag(), null, message.getAuthor().getEffectiveAvatarUrl())
				.setTitle("Not Enough Time on Server")
				.setColor(Bot.config.get(message.getGuild()).getSlashCommand().getErrorColor())
				.addField("Author", message.getAuthor().getAsMention(), true)
				.addField("Channel", message.getChannel().getAsMention(), true)
				.addField("Created at", String.format("<t:%s:F>", epoch), true)
				.addField("Attachment Links", String.join("\n", links), true)
				.setFooter("ID: " + message.getIdLong())
				.build();
	}

	private boolean isSuggestionsChannel(TextChannel channel) {
		return channel.equals(Bot.config.get(channel.getGuild()).getModeration().getSuggestionChannel());
	}
}