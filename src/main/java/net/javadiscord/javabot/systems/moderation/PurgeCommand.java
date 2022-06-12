package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.moderation.ModerateCommand;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.TimeUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Moderation command that deletes multiple messages from a single channel.
 */
public class PurgeCommand extends ModerateCommand {
	private final Path ARCHIVE_DIR = Path.of("purgeArchives");

	public PurgeCommand() {
		setSlashCommandData(Commands.slash("purge", "Deletes messages from a channel.")
				.addOption(OptionType.INTEGER, "amount", "Number of messages to remove.", true)
				.addOption(OptionType.USER, "user", "The user whose messages to remove. If left blank, messages from any user are removed.", false)
				.addOption(OptionType.BOOLEAN, "archive", "Whether the removed messages should be saved in an archive. This defaults to true, if left blank.", false)
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationCommand(SlashCommandInteractionEvent event, Member commandUser) {
		this.setAllowThreads(true);
		OptionMapping amountOption = event.getOption("amount");
		OptionMapping userOption = event.getOption("user");
		boolean archive = event.getOption("archive", true, OptionMapping::getAsBoolean);

		ModerationConfig config = Bot.config.get(event.getGuild()).getModeration();
		Long amount = (amountOption == null) ? null : amountOption.getAsLong();
		User user = (userOption == null) ? null : userOption.getAsUser();
		int maxAmount = config.getPurgeMaxMessageCount();
		if (amount == null || amount < 1 || amount > maxAmount) {
			return Responses.warning(event, "Invalid amount. Should be between 1 and " + maxAmount + ", inclusive.");
		}
		Bot.asyncPool.submit(() -> this.purge(amount, user, event.getUser(), archive, event.getTextChannel(), config.getLogChannel()));
		StringBuilder sb = new StringBuilder();
		sb.append(amount > 1 ? "Up to " + amount + " messages " : "1 message ");
		if (user != null) {
			sb.append("by the user ").append(user.getAsTag()).append(' ');
		}
		sb.append("will be removed").append(archive ? " and placed in an archive." : '.');
		return Responses.info(event, "Purge Started", sb.toString());
	}

	/**
	 * Purges messages from a channel.
	 *
	 * @param amount      The number of messages to remove.
	 * @param user        The user whose messages to remove. If null, messages from any
	 *                    user are removed.
	 * @param initiatedBy The user which initiated the purge.
	 * @param archive     Whether to create an archive file for the purge.
	 * @param channel     The channel to remove messages from.
	 * @param logChannel  The channel to write log messages to during the purge.
	 */
	private void purge(@Nullable Long amount, @Nullable User user, User initiatedBy, boolean archive, MessageChannel channel, TextChannel logChannel) {
		MessageHistory history = channel.getHistory();
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		String file = String.format("purge_%s_%s.txt", channel.getName(), timestamp);
		PrintWriter archiveWriter = archive ? createArchiveWriter(channel, logChannel, file) : null;
		List<Message> messages;
		OffsetDateTime startTime = OffsetDateTime.now();
		long count = 0;
		logChannel.sendMessageFormat("Starting purge of channel %s, initiated by %s", channel.getAsMention(), initiatedBy.getAsMention())
				.queue();
		do {
			messages = history.retrievePast(amount == null ? 100 : (int) Math.min(100, amount)).complete();
			if (!messages.isEmpty()) {
				int messagesRemoved = removeMessages(messages, user, archiveWriter);
				count += messagesRemoved;
				logChannel.sendMessage(String.format(
						"Removed **%d** messages from %s; a total of **%d** messages have been removed in this purge so far.",
						messagesRemoved,
						channel.getAsMention(),
						count
				)).queue();
			}
		} while (!messages.isEmpty() && (amount == null || amount > count));
		if (archiveWriter != null) {
			archiveWriter.close();
		}
		MessageAction action = logChannel.sendMessage(String.format(
				"Purge of channel %s has completed. %d messages have been removed, and the purge took %s.",
				channel.getAsMention(),
				count,
				new TimeUtils().formatDurationToNow(startTime)
		));
		if (archive) action.addFile(ARCHIVE_DIR.resolve(file).toFile());
		action.queue();
	}

	/**
	 * Deletes the given messages. If user is not null, only messages from that
	 * user are deleted. If the given archive writer is not null, the message
	 * will be recorded in the archive.
	 *
	 * @param messages      The messages to remove.
	 * @param user          The user to remove messages for.
	 * @param archiveWriter The writer to write message archive info to.
	 * @return The number of messages that were actually deleted.
	 */
	private int removeMessages(List<Message> messages, @Nullable User user, @Nullable PrintWriter archiveWriter) {
		int messagesRemoved = 0;
		for (Message msg : messages) {
			if (user == null || msg.getAuthor().equals(user)) {
				msg.delete().complete();
				messagesRemoved++;
				if (archiveWriter != null) {
					archiveMessage(archiveWriter, msg);
				}
			}
		}
		return messagesRemoved;
	}

	/**
	 * Creates a new {@link PrintWriter} which can be used to record information
	 * about purged messages from a channel.
	 *
	 * @param channel    The channel to create the writer for.
	 * @param logChannel The log channel, where log messages can be sent.
	 * @param file       The archive's filename.
	 * @return The print writer to use.
	 */
	private PrintWriter createArchiveWriter(MessageChannel channel, TextChannel logChannel, String file) {
		try {
			if (Files.notExists(ARCHIVE_DIR)) Files.createDirectory(ARCHIVE_DIR);
			Path archiveFile = ARCHIVE_DIR.resolve(file);
			var archiveWriter = new PrintWriter(Files.newBufferedWriter(archiveFile), true);
			logChannel.sendMessageFormat("Created archive of purge of channel %s at `%s`", channel.getAsMention(), archiveFile).queue();
			archiveWriter.println("Purge of channel " + channel.getName());
			return archiveWriter;
		} catch (IOException e) {
			logChannel.sendMessage("Could not create archive file for purge of channel " + channel.getAsMention() + ".").queue();
			return null;
		}
	}

	/**
	 * Appends information about a message to a writer.
	 *
	 * @param writer  The writer to use to write data.
	 * @param message The message to get information from.
	 */
	private void archiveMessage(PrintWriter writer, Message message) {
		writer.printf(
				"%s : Removing message %s by %s which was sent at %s\n--- Text ---\n%s\n--- End Text ---\n\n",
				OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
				message.getId(),
				message.getAuthor().getAsTag(),
				message.getTimeCreated().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
				message.getContentRaw()
		);
	}
}