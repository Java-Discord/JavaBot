package net.discordjug.javabot.systems.user_commands.format_code;

import net.discordjug.javabot.util.*;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Shared sending logic for the code-formatting commands. Replies with the full code as a
 * downloadable file, then posts it as one or more ordered code-block messages that each respect
 * Discord's 2000-character limit.
 */
class FormatCodeDispatcher {

	/**
	 * The maximum number of code-block messages to post inline; longer code results in an error.
	 */
	private static final int MAX_MESSAGES = 5;

	/**
	 * Acknowledges the interaction by replying with the full code as a file, then posts the code as
	 * ordered code-block messages. Replies with an error instead if there is nothing to format.
	 *
	 * @param code   the code to send
	 * @param event  the interaction to reply to
	 * @param target the original message the code came from, used for the channel and the
	 *               "View Original" / delete buttons
	 */
	public static void sendCode(Code code, @Nonnull CommandInteraction event, Message target){
		if (code.getContent().isBlank()) {
			Responses.errorWithTitle(event, "404 Code not found","There is no code to format in that message.").queue();
			return;
		}

		List<String> messages = code.toDiscordMessages();

		MessageChannel channel = target.getChannel();

		if (messages.size() > MAX_MESSAGES) {
			Responses.errorWithTitle(event.getHook(), "Output Too Large", "The formatted result is too large to send. Please provide a smaller code snippet or use a paste service instead."
			).queue();
			return;
		}

		Responses.success(event, "Success", "The formatted message is being sent to this channel.")
				.queue(success -> sendChunksInOrder(channel, messages, 0, target,event));
	}


	private static void sendChunksInOrder(MessageChannel channel, List<String> messages, int index, Message target, @Nonnull CommandInteraction event) {
		if (index >= messages.size()) {
			return;
		}
		var action = channel.sendMessage(messages.get(index))
				.setAllowedMentions(List.of());

		if (index == messages.size() - 1) {
			if(index == 0){
				action.setComponents(buildActionRow(target, event.getUser().getIdLong()));
			} else {
				action.setComponents(buildActionRow(target));
			}
		}

		action.queue(success ->
				sendChunksInOrder(channel, messages, index + 1, target, event));
	}

	/**
	 * Builds the action row placed on the last code-block message.
	 *
	 * @param target      the original message linked by the "View Original" button
	 * @return an action row containing the "View Original" link button
	 */
	@Contract("_ -> new")
	static @NotNull ActionRow buildActionRow(@NotNull Message target) {
		return ActionRow.of(Button.link(target.getJumpUrl(), "View Original"));
	}

	/**
	 * Builds the action row placed on the file-upload message: a delete button and a "View Original" link.
	 *
	 * @param target      the original message linked by the "View Original" button
	 * @param requesterId the id of the user permitted to delete the message
	 * @return an action row containing the delete and "View Original" buttons
	 */
	@Contract("_,_ -> new")
	static @NotNull ActionRow buildActionRow(@NotNull Message target, long requesterId) {
		return ActionRow.of(InteractionUtils.createDeleteButton(requesterId),
				Button.link(target.getJumpUrl(), "View Original"));
	}
}
