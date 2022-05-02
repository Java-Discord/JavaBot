package net.javadiscord.javabot.systems.help;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.systems.help.model.ChannelReservation;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.*;

/**
 * This listener is responsible for handling messages that are sent in one or
 * more designated help channels.
 */
@Slf4j
public class HelpChannelListener extends ListenerAdapter {

	/**
	 * A static Map that holds all messages that was sent in a specific reserved channel.
	 */
	public static Map<Long, List<Message>> reservationMessages = new HashMap<>();

	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem() || event.getChannelType() != ChannelType.TEXT) {
			return;
		}
		HelpConfig config = Bot.config.get(event.getGuild()).getHelp();
		TextChannel channel = event.getTextChannel();
		HelpChannelManager manager = new HelpChannelManager(config);

		// If a message was sent in an open text channel, reserve it.
		Category openChannelCategory = config.getOpenChannelCategory();
		if (openChannelCategory == null) {
			log.error("Could not find Open Help Category for Guild {}", event.getGuild().getName());
			return;
		}
		if (openChannelCategory.equals(channel.getParentCategory())) {
			if (manager.mayUserReserveChannel(event.getAuthor())) {
				try {
					manager.reserve(channel, event.getAuthor(), event.getMessage());
				} catch (SQLException e) {
					e.printStackTrace();
					channel.sendMessage("An error occurred and this channel could not be reserved.").queue();
				}
			} else {
				event.getMessage().reply(config.getReservationNotAllowedMessage()).queue();
			}
		} else if (config.getReservedChannelCategory().equals(channel.getParentCategory())) {
			Optional<ChannelReservation> reservationOptional = manager.getReservationForChannel(event.getChannel().getIdLong());
			reservationOptional.ifPresent(reservation -> {
				List<Message> messages = new ArrayList<>();
				messages.add(event.getMessage());
				if (reservationMessages.containsKey(reservation.getId())) {
					messages.addAll(reservationMessages.get(reservation.getId()));
				}
				reservationMessages.put(reservation.getId(), messages);
			});
		} else if (config.getDormantChannelCategory().equals(channel.getParentCategory())) {
			// Prevent anyone from sending messages in dormant channels.
			event.getMessage().delete().queue();
		}
	}
}
