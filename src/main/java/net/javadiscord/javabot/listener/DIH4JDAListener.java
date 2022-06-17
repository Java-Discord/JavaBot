package net.javadiscord.javabot.listener;

import com.dynxsty.dih4jda.events.DIH4JDAListenerAdapter;
import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.ModalInteraction;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.javadiscord.javabot.Bot;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public class DIH4JDAListener extends DIH4JDAListenerAdapter {
	@Override
	public void onCommandException(CommandInteraction interaction, Exception e) {
		e.printStackTrace();
		Sentry.captureException(e);
		handleReply(interaction, buildExceptionEmbed(interaction.getGuild(), e));
	}

	@Override
	public void onComponentException(ComponentInteraction interaction, Exception e) {
		e.printStackTrace();
		Sentry.captureException(e);
		handleReply(interaction, buildExceptionEmbed(interaction.getGuild(), e));
	}

	@Override
	public void onModalException(ModalInteraction interaction, Exception e) {
		e.printStackTrace();
		Sentry.captureException(e);
		handleReply(interaction, buildExceptionEmbed(interaction.getGuild(), e));
	}

	@Override
	public void onInvalidUser(CommandInteraction interaction, Set<Long> userIds) {
		handleReply(interaction, buildNoAccessEmbed(interaction.getGuild()));
	}

	@Override
	public void onInvalidRole(CommandInteraction interaction, Set<Long> userIds) {
		handleReply(interaction, buildNoAccessEmbed(interaction.getGuild()));
	}

	@Override
	public void onInsufficientPermissions(CommandInteraction interaction, Set<Permission> permissions) {
		handleReply(interaction, buildInsufficientPermissionsEmbed(interaction.getGuild(), permissions));
	}

	/**
	 * Handles the reply to the {@link IReplyCallback} and acknowledges it, if not already done.
	 *
	 * @param callback The {@link IReplyCallback} to reply to.
	 * @param embed The {@link MessageEmbed} to send.
	 */
	private void handleReply(IReplyCallback callback, MessageEmbed embed) {
		if (!callback.isAcknowledged()) {
			callback.deferReply(true).queue();
		}
		callback.getHook().sendMessageEmbeds(embed).queue();
	}

	private EmbedBuilder buildErrorEmbed(Guild guild) {
		return new EmbedBuilder()
				.setTitle("An error occurred!")
				.setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
				.setTimestamp(Instant.now());
	}

	private MessageEmbed buildExceptionEmbed(Guild guild, Exception e) {
		return buildErrorEmbed(guild)
				.setDescription("An unexpected error occurred!")
				.setFooter(e.getClass().getSimpleName())
				.build();
	}

	private MessageEmbed buildInsufficientPermissionsEmbed(Guild guild, Set<Permission> permissions) {
		String perms = permissions.stream().map(Permission::getName).collect(Collectors.joining(", "));
		return buildErrorEmbed(guild)
				.setDescription(String.format(
						"You're not allowed to use this command. " +
								"In order to execute this command, you'll need the following permissions: `%s`", perms))
				.build();
	}

	private MessageEmbed buildNoAccessEmbed(Guild guild) {
		return buildErrorEmbed(guild)
				.setDescription("You're not allowed to use this command.")
				.build();
	}
}
