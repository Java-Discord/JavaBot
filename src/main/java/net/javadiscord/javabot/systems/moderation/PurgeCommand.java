package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
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
 * This command deletes messages from a channel.
 */
public class PurgeCommand implements ISlashCommand {

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		Member member = event.getMember();
		if (member == null) {
			return Responses.warning(event, "This command can only be used in a guild.");
		}
		var config = Bot.config.get(event.getGuild()).getModeration();

		OptionMapping amountOption = event.getOption("amount");
		OptionMapping userOption = event.getOption("user");
		OptionMapping archiveOption = event.getOption("archive");

		Long amount = (amountOption == null) ? null : amountOption.getAsLong();
		User user = (userOption == null) ? null : userOption.getAsUser();
		boolean archive = archiveOption != null && archiveOption.getAsBoolean();
		int maxAmount = config.getPurgeMaxMessageCount();

		if (amount != null && (amount < 1 || amount > maxAmount)) {
			return Responses.warning(event, "Invalid amount. If specified, should be between 1 and " + maxAmount + ", inclusive.");
		}

		Bot.asyncPool.submit(() -> this.purge(amount, user, archive, event.getTextChannel(), config.getLogChannel()));
		StringBuilder sb = new StringBuilder();
		sb.append(amount != null ? (amount > 1 ? "Up to " + amount + " messages " : "1 message ") : "All messages ");
		if (user != null) {
			sb.append("by the user ").append(user.getAsTag()).append(' ');
		}
		sb.append("will be removed").append(archive ? " and placed in an archive." : '.');
		return Responses.info(event, "Purge Started", sb.toString());
	}

	/**
	 * Purges messages from a channel.
	 *
	 * @param amount     The number of messages to remove. If null, all messages
	 *                   will be removed.
	 * @param user       The user whose messages to remove. If null, messages from any
	 *                   user are removed.
	 * @param archive    Whether to create an archive file for the purge.
	 * @param channel    The channel to remove messages from.
	 * @param logChannel The channel to write log messages to during the purge.
	 */
	private void purge(@Nullable Long amount, @Nullable User user, boolean archive, TextChannel channel, TextChannel logChannel) {
		MessageHistory history = channel.getHistory();
		PrintWriter archiveWriter = archive ? createArchiveWriter(channel, logChannel) : null;
		List<Message> messages;
		OffsetDateTime startTime = OffsetDateTime.now();
		long count = 0;
		logChannel.sendMessage("Starting purge of channel " + channel.getAsMention()).queue();
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
		logChannel.sendMessage(String.format(
				"Purge of channel %s has completed. %d messages have been removed, and the purge took %s.",
				channel.getAsMention(),
				count,
				new TimeUtils().formatDurationToNow(startTime)
		)).queue();
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
	 * @return The print writer to use.
	 */
	private PrintWriter createArchiveWriter(TextChannel channel, TextChannel logChannel) {
		try {
			Path purgeArchivesDir = Path.of("purgeArchives");
			if (Files.notExists(purgeArchivesDir)) Files.createDirectory(purgeArchivesDir);
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
			Path archiveFile = purgeArchivesDir.resolve("purge_" + channel.getName() + "_" + timestamp + ".txt");
			var archiveWriter = new PrintWriter(Files.newBufferedWriter(archiveFile), true);
			logChannel.sendMessage("Created archive of purge of channel " + channel.getAsMention() + " at " + archiveFile).queue();
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