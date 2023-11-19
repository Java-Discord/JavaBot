package net.discordjug.javabot.listener;

import xyz.dynxsty.dih4jda.events.CommandExceptionEvent;
import xyz.dynxsty.dih4jda.events.ComponentExceptionEvent;
import xyz.dynxsty.dih4jda.events.DIH4JDAEventListener;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.events.InsufficientPermissionsEvent;
import xyz.dynxsty.dih4jda.events.InvalidRoleEvent;
import xyz.dynxsty.dih4jda.events.InvalidUserEvent;
import xyz.dynxsty.dih4jda.events.ModalExceptionEvent;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listener class for various events provided by {@link xyz.dynxsty.dih4jda.DIH4JDA}.
 */
public class DIH4JDAListener implements DIH4JDAEventListener {

	@Override
	public void onCommandException(@NotNull CommandExceptionEvent event) {
		ExceptionLogger.capture(event.getThrowable(), getClass().getSimpleName());
		handleReply(event.getInteraction(), buildExceptionEmbed(event.getThrowable()));
	}

	@Override
	public void onComponentException(@NotNull ComponentExceptionEvent event) {
		ExceptionLogger.capture(event.getThrowable(), getClass().getSimpleName());
		handleReply(event.getInteraction(), buildExceptionEmbed(event.getThrowable()));
	}

	@Override
	public void onModalException(@NotNull ModalExceptionEvent event) {
		ExceptionLogger.capture(event.getThrowable(), getClass().getSimpleName());
		handleReply(event.getInteraction(), buildExceptionEmbed(event.getThrowable()));
	}

	@Override
	public void onInvalidUser(@NotNull InvalidUserEvent event) {
		handleReply(event.getInteraction(), buildNoAccessEmbed());
	}

	@Override
	public void onInvalidRole(@NotNull InvalidRoleEvent event) {
		handleReply(event.getInteraction(), buildNoAccessEmbed());
	}

	@Override
	public void onInsufficientPermissions(@NotNull InsufficientPermissionsEvent event) {
		handleReply(event.getInteraction(), buildInsufficientPermissionsEmbed(event.getPermissions()));
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

	private @NotNull MessageEmbed buildExceptionEmbed(@NotNull Throwable throwable) {
		return buildErrorEmbed()
				.setDescription(throwable.getMessage() == null ? "An error occurred." : MarkdownUtil.codeblock(throwable.getMessage()))
				.setFooter(throwable.getClass().getSimpleName())
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
