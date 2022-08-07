package net.javadiscord.javabot.systems.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Handles all sorts of user notifications.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class UserNotificationService extends NotificationService.MessageChannelNotification {
	private final User user;

	/**
	 * Sends a notification to a {@link User}s' {@link net.dv8tion.jda.api.entities.PrivateChannel}.
	 *
	 * @param function The {@link Function} to use which MUST return a {@link MessageAction}.
	 */
	public void sendDirectMessage(@NotNull Function<MessageChannel, MessageAction> function) {
		user.openPrivateChannel().queue(
				channel -> send(channel, function),
				error -> log.error("Could not open PrivateChannel with user " + user.getAsTag(), error)
		);
	}
}
