package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for message actions.
 */
public class MessageActionUtils {

	private MessageActionUtils() {
	}

	/**
	 * Converts a {@link List} of Message {@link ItemComponent}s into a List of actions rows.
	 *
	 * @param components The {@link List} of {@link ItemComponent}s.
	 * @return A {@link List} of {@link ActionRow}s.
	 */
	public static List<ActionRow> toActionRows(List<? extends ItemComponent> components) {
		if (components.size() > 25) {
			throw new IllegalArgumentException("Cannot add more than 25 components to a message action.");
		}
		List<ActionRow> rows = new ArrayList<>(5);
		List<ItemComponent> rowComponents = new ArrayList<>(5);
		while (!components.isEmpty()) {
			rowComponents.add(components.remove(0));
			if (rowComponents.size() == 5) {
				rows.add(ActionRow.of(rowComponents));
				rowComponents.clear();
			}
		}
		if (!rowComponents.isEmpty()) {
			rows.add(ActionRow.of(rowComponents));
		}
		return rows;
	}

	public static List<ActionRow> enableActionRows(List<ActionRow> actionRows) {
		return actionRows.stream().map(ActionRow::asEnabled).toList();
	}

	public static List<ActionRow> disableActionRows(List<ActionRow> actionRows) {
		return actionRows.stream().map(ActionRow::asDisabled).toList();
	}

	/**
	 * Adds all Attachments from the initial message to the new message action and sends the message.
	 *
	 * @param message The initial {@link Message} object.
	 * @param action  The new {@link MessageAction}.
	 * @return A {@link CompletableFuture} with the message that is being sent.
	 */
	public static CompletableFuture<Message> addAttachmentsAndSend(Message message, MessageAction action) {
		List<CompletableFuture<?>> attachmentFutures = new ArrayList<>();
		for (Message.Attachment attachment : message.getAttachments()) {
			attachmentFutures.add(
					attachment.getProxy().download()
							.thenApply(is -> action.addFile(is, attachment.getFileName()))
							.exceptionally(e -> action.append("Could not add Attachment: " + attachment.getFileName()))
			);
		}
		return CompletableFuture.allOf(attachmentFutures.toArray(new CompletableFuture<?>[0]))
				.thenCompose(unusedActions -> action.submit());
	}

	/**
	 * Sends a message with an embed, creates a thread from that message and copies all messages to the thread.
	 * @param targetChannel The channel to send the embed/create the thread in.
	 * @param infoEmbed The embed to send.
	 * @param newThreadName The name of the thread.
	 * @param messages The messages to copy.
	 * @param onFinish A callback to execute when copying is done.
	 */
	public static void copyMessagesToNewThread(GuildMessageChannel targetChannel, @NotNull MessageEmbed infoEmbed, String newThreadName, List<Message> messages, Runnable onFinish) {
		targetChannel.sendMessageEmbeds(infoEmbed).queue(
				message -> message.createThreadChannel(newThreadName).queue(
						thread -> {
							messages.forEach(m -> {
								String messageContent = m.getContentRaw();
								if (messageContent.trim().length() == 0) messageContent = "[attachment]";
								MessageActionUtils.addAttachmentsAndSend(m, thread.sendMessage(messageContent)
										.allowedMentions(EnumSet.of(Message.MentionType.EMOJI, Message.MentionType.CHANNEL)));
							});
							onFinish.run();
						}
				));
	}
}
