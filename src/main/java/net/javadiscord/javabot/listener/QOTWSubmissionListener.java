package net.javadiscord.javabot.listener;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.util.InteractionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for new messages inside QOTW submissions and warns the author about the webhook limitations.
 */
@RequiredArgsConstructor
public class QOTWSubmissionListener extends ListenerAdapter {
	private final BotConfig botConfig;

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (!event.isFromGuild() || !event.isFromThread() || event.getChannelType() != ChannelType.GUILD_PRIVATE_THREAD) {
			return;
		}
		QOTWConfig qotwConfig = botConfig.get(event.getGuild()).getQotwConfig();
		ThreadChannel thread = event.getChannel().asThreadChannel();
		// TODO: fix check
		if (thread.getParentChannel().getIdLong() != qotwConfig.getSubmissionChannelId()) {
			return;
		}
		if (event.getMessage().getContentRaw().length() > 2000) {
			event.getChannel().sendMessageFormat("""
									Hey %s!
									Please keep in mind that messages **over 2000 characters** get split in half due to webhook limitations.
									If you want to make sure that your submission is properly formatted, split your message into smaller chunks instead.""",
							event.getAuthor().getAsMention())
					.setActionRow(Button.secondary(InteractionUtils.DELETE_ORIGINAL_TEMPLATE, "\uD83D\uDDD1️"))
					.queue();
		}
	}
}
