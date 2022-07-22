package net.javadiscord.javabot.systems.notification;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;


/**
 * Abstract class used for sending Notifications to Discord Users and Channels.
 */
@Slf4j
public abstract class NotificationService {
	void sendDirectMessageNotification(User user, MessageEmbed message) {
		user.openPrivateChannel().queue(
				channel -> sendMessageChannelNotification(channel, message),
				error -> log.warn("Could not send private Notification to User " + user.getAsTag())
		);
	}

	void sendDirectMessageNotification(User user, String s, Object... args) {
		user.openPrivateChannel().queue(
				channel -> sendMessageChannelNotification(channel, s, args),
				error -> log.warn("Could not send private Notification to User " + user.getAsTag())
		);
	}

	void sendMessageChannelNotification(MessageChannel channel, MessageEmbed message) {
		channel.sendMessageEmbeds(message).queue(s -> {}, e -> log.warn("Could not send embed to channel " + channel.getName()));
	}

	void sendMessageChannelNotification(MessageChannel channel, String s, Object... args) {
		channel.sendMessageFormat(s, args).queue(success -> {}, e -> log.warn("Could not send message to channel " + channel.getName()));
	}
}
