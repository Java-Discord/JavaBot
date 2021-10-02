package com.javadiscord.javabot.help;

import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.interactions.ButtonImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;

import java.time.OffsetDateTime;

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
		for (var channel : config.getReservedChannelCategory().getTextChannels()) {
			this.checkReservedChannel(channel).queue();
		}
		for (var channel : config.getOpenChannelCategory().getTextChannels()) {
			this.checkOpenChannel(channel).queue();
		}
		this.balanceChannels();
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
	 *         channel will be unreserved.
	 *     </li>
	 *     <li>
	 *         If for some reason we can't retrieve the owner of the channel,
	 *         like if they left the server, the channel will be unreserved.
	 *     </li>
	 * </ul>
	 * @param channel The channel to check.
	 * @return A rest action that completes when the check is done.
	 */
	private RestAction<?> checkReservedChannel(TextChannel channel) {
		User owner = this.channelManager.getReservedChannelOwner(channel);
		if (owner == null) {
			log.info("Unreserving channel {} because no owner could be found.", channel.getAsMention());
			return this.channelManager.unreserveChannel(channel);
		}
		return channel.getHistory().retrievePast(1).map(messages -> {
			Message mostRecentMessage = messages.isEmpty() ? null : messages.get(0);
			if (mostRecentMessage == null) {
				log.info("Unreserving channel {} because no recent messages could be found.", channel.getAsMention());
				return this.channelManager.unreserveChannel(channel);
			}

			// Check if the most recent message is a channel inactivity check, and check that it's old enough to surpass the remove timeout.
			if (
				mostRecentMessage.getAuthor().equals(this.jda.getSelfUser()) &&
				mostRecentMessage.getContentRaw().contains("Are you finished with this channel?") &&
				mostRecentMessage.getTimeCreated().plusMinutes(config.getRemoveTimeoutMinutes()).isBefore(OffsetDateTime.now())
			) {
				log.info("Unreserving channel {} because of inactivity for {} minutes following inactive check.", channel.getAsMention(), config.getRemoveTimeoutMinutes());
				return RestAction.allOf(
						mostRecentMessage.delete(),
						channel.sendMessage(String.format(
								"%s, this channel will be unreserved due to prolonged inactivity. If your question still isn't answered, please ask again in an open channel.",
								owner.getAsMention()
						)),
						this.channelManager.unreserveChannel(channel)
				);
			}

			// The most recent message is not an activity check, so check if it's old enough to warrant sending an activity check.
			if (mostRecentMessage.getTimeCreated().plusMinutes(config.getInactivityTimeoutMinutes()).isBefore(OffsetDateTime.now())) {
				log.info("Sending inactivity check to {} because of no activity after {} minutes.", channel.getAsMention(), config.getInactivityTimeoutMinutes());
				return channel.sendMessage(String.format(
						"Hey %s, it looks like this channel is inactive. Are you finished with this channel?\n\n> _If no response is received after %d minutes, this channel will be removed._",
						owner.getAsMention(),
						config.getRemoveTimeoutMinutes()
					))
					.setActionRow(
						new ButtonImpl("help-channel:done", "Yes, I'm done here!", ButtonStyle.SUCCESS, false, null),
						new ButtonImpl("help-channel:not-done", "No, I'm still using it.", ButtonStyle.DANGER, false, null)
					);
			}
			// No action needed.
			return new CompletedRestAction<>(this.jda, null);
		}).complete();
	}

	/**
	 * Checks an open help channel to ensure it's in the correct state.
	 * @param channel The channel to check.
	 * @return A rest action that completes when the check is done.
	 */
	private RestAction<?> checkOpenChannel(TextChannel channel) {
		if (!this.config.isRecycleChannels()) {
			return channel.getHistory().retrievePast(1).map(messages -> {
				var lastMessage = messages.isEmpty() ? null : messages.get(0);
				if (lastMessage != null) {
					// If we're not recycling channels, we want to keep all open channels fresh.
					// Any open channel with a message in it should be immediately become reserved.
					// However, network issues or other things could cause this to fail, so we clean up here.
					log.info("Removing non-empty open channel {}.", channel.getAsMention());
					return channel.delete();
				} else {
					return new CompletedRestAction<>(this.jda, null);
				}
			});
		} else {
			return new CompletedRestAction<>(this.jda, null);
		}
	}

	/**
	 * Tries to move channels around to attain the preferred open channel count.
	 */
	private void balanceChannels() {
		int openChannelCount = this.channelManager.getOpenChannelCount();
		while (openChannelCount < this.config.getPreferredOpenChannelCount()) {
			if (this.config.isRecycleChannels()) {
				var dormantChannels = this.config.getDormantChannelCategory().getTextChannels();
				if (!dormantChannels.isEmpty()) {
					dormantChannels.get(0).getManager().setParent(this.config.getOpenChannelCategory()).queue();
					openChannelCount++;
				} else {
					log.warn("Could not find a dormant channel to replenish open channels.");
					break;
				}
			} else {
				channelManager.openNew();
				openChannelCount++;
			}
		}
		while (openChannelCount > this.config.getPreferredOpenChannelCount()) {
			var channel = this.config.getOpenChannelCategory().getTextChannels().get(0);
			if (this.config.isRecycleChannels()) {
				channel.getManager().setParent(this.config.getDormantChannelCategory()).queue();
			} else {
				channel.delete().queue();
			}
			openChannelCount--;
		}
	}
}
