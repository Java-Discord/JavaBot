package net.javadiscord.javabot.listener;

import com.dynxsty.dih4jda.events.DIH4JDAEventListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.ModalInteraction;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listener class for various events provided by {@link com.dynxsty.dih4jda.DIH4JDA}.
 */
public class DIH4JDAListener implements DIH4JDAEventListener {
	@Override
	public void onCommandException(CommandInteraction interaction, Exception e) {
		ExceptionLogger.capture(e, getClass().getSimpleName());
		handleReply(interaction, buildExceptionEmbed(e));
	}

	@Override
	public void onComponentException(ComponentInteraction interaction, Exception e) {
		ExceptionLogger.capture(e, getClass().getSimpleName());
		handleReply(interaction, buildExceptionEmbed(e));
	}

	@Override
	public void onModalException(ModalInteraction interaction, Exception e) {
		ExceptionLogger.capture(e, getClass().getSimpleName());
		handleReply(interaction, buildExceptionEmbed(e));
	}

	@Override
	public void onInvalidUser(CommandInteraction interaction, Set<Long> userIds) {
		handleReply(interaction, buildNoAccessEmbed());
	}

	@Override
	public void onInvalidRole(CommandInteraction interaction, Set<Long> userIds) {
		handleReply(interaction, buildNoAccessEmbed());
	}

	@Override
	public void onInsufficientPermissions(CommandInteraction interaction, Set<Permission> permissions) {
		handleReply(interaction, buildInsufficientPermissionsEmbed(permissions));
	}

	/**
	 * Handles the reply to the {@link IReplyCallback} and acknowledges it, if not already done.
	 *
	 * @param callback The {@link IReplyCallback} to reply to.
	 * @param embed The {@link MessageEmbed} to send.
	 */
	private void handleReply(@NotNull IReplyCallback callback, MessageEmbed embed) {
		if (!callback.isAcknowledged()) {
			callback.deferReply(true).queue();
		}
		callback.getHook().sendMessageEmbeds(embed).queue();
	}

	private @NotNull EmbedBuilder buildErrorEmbed() {
		return new EmbedBuilder()
				.setTitle("An Error occurred!")
				.setColor(Responses.Type.ERROR.getColor())
				.setTimestamp(Instant.now());
	}

	private @NotNull MessageEmbed buildExceptionEmbed(@NotNull Exception e) {
		return buildErrorEmbed()
				.setDescription(MarkdownUtil.codeblock(e.getMessage()))
				.setFooter(e.getClass().getSimpleName())
				.build();
	}

	private @NotNull MessageEmbed buildInsufficientPermissionsEmbed(@NotNull Set<Permission> permissions) {
		String perms = permissions.stream().map(Permission::getName).collect(Collectors.joining(", "));
		return buildErrorEmbed()
				.setDescription(String.format(
						"You're not allowed to use this command. " +
								"In order to execute this command, you'll need the following permissions: `%s`", perms))
				.build();
	}

	private @NotNull MessageEmbed buildNoAccessEmbed() {
		return buildErrorEmbed()
				.setDescription("You're not allowed to use this command.")
				.build();
	}
}
