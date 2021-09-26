package com.javadiscord.javabot.help;

import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.internal.interactions.ButtonImpl;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Task that updates all help channels in a particular guild.
 */
@Slf4j
public class HelpChannelUpdater implements Runnable {
	private final JDA jda;
	private final HelpConfig config;
	private final HelpChannelManager channelManager;

	public HelpChannelUpdater(JDA jda, HelpConfig config) {
		this.jda = jda;
		this.config = config;
		this.channelManager = new HelpChannelManager(config);
	}


	@Override
	public void run() {
		var category = config.getHelpChannelCategory();
		if (category == null) throw new IllegalStateException("Missing required help channel category. Cannot update help channels.");
		var channels = category.getTextChannels();
		int openChannelCount = 0;
		for (var channel : channels) {
			if (channelManager.isReserved(channel)) {
				this.checkReservedChannel(channel);
			} else if (channelManager.isOpen(channel) && this.checkOpenChannel(channel)) {
				openChannelCount++;
			}
		}
		while (openChannelCount < config.getPreferredOpenChannelCount()) {
			channelManager.openNew();
			openChannelCount++;
		}
	}

	/**
	 * Performs checks on a reserved channel. This will do several things:
	 * <ul>
	 *     <li>
	 *         If the most recent message in the channel is old enough, it will
	 *         send an activity check message in the channel, asking the user to
	 *         confirm whether they're still using it.
	 *     </li>
	 *     <li>
	 *         If the most recent message is an activity check message, and it
	 *         has stuck around long enough without any response, then the
	 *         channel will be removed.
	 *     </li>
	 *     <li>
	 *         If for some reason we can't retrieve the owner of the channel,
	 *         like if they left the server, the channel will be removed.
	 *     </li>
	 * </ul>
	 * @param channel The channel to check.
	 */
	private void checkReservedChannel(TextChannel channel) {
		User owner = this.channelManager.getReservedChannelOwner(channel);
		if (owner == null) {
			log.info("Removing reserved channel {} because no owner could be found.", channel.getAsMention());
			channel.delete().queue();
			return;
		}
		channel.getHistory().retrievePast(1).queue(messages -> {
			Message mostRecentMessage = messages.isEmpty() ? null : messages.get(0);
			if (mostRecentMessage == null) {
				log.info("Removing reserved channel {} because no recent messages could be found.", channel.getAsMention());
				channel.delete().queue();
				return;
			}

			// Check if the most recent message is a channel inactivity check, and check that it's old enough to surpass the remove timeout.
			if (
				mostRecentMessage.getAuthor().equals(this.jda.getSelfUser()) &&
				mostRecentMessage.getContentRaw().contains("Are you finished with this channel?") &&
				mostRecentMessage.getTimeCreated().plusMinutes(config.getRemoveTimeoutMinutes()).isBefore(OffsetDateTime.now())
			) {
				log.info("Removing reserved channel {} because of inactivity for {} minutes following inactive check.", channel.getAsMention(), config.getRemoveTimeoutMinutes());
				channel.sendMessage(String.format(
						"%s, this channel will be closed in 30 seconds due to prolonged inactivity. If your question still isn't answered, please ask again in an open channel.",
						owner.getAsMention()
				)).queue();
				channel.delete().queueAfter(30, TimeUnit.SECONDS);
				return;
			}

			// The most recent message is not an activity check, so check if it's old enough to warrant sending an activity check.
			if (mostRecentMessage.getTimeCreated().plusMinutes(config.getInactivityTimeoutMinutes()).isBefore(OffsetDateTime.now())) {
				log.info("Sending inactivity check to {} because of no activity after {} minutes.", channel.getAsMention(), config.getInactivityTimeoutMinutes());
				channel.sendMessage(String.format(
						"Hey %s, it looks like this channel is inactive. Are you finished with this channel?\n\n> _If no response is received after %d minutes, this channel will be removed._",
						owner.getAsMention(),
						config.getRemoveTimeoutMinutes()
					))
					.setActionRow(
						new ButtonImpl("help-channel:done", "Yes, I'm done here!", ButtonStyle.SUCCESS, false, null),
						new ButtonImpl("help-channel:not-done", "No, I'm still using it.", ButtonStyle.DANGER, false, null)
					)
					.queue();
			}
		});
	}

	private boolean checkOpenChannel(TextChannel channel) {
		boolean isEmpty = channel.getHistoryFromBeginning(1).complete().isEmpty();
		if (!isEmpty) {
			log.info("Removing non-empty open channel {}.", channel.getAsMention());
			channel.delete().complete();
			return false;
		}
		return true;
	}
}
