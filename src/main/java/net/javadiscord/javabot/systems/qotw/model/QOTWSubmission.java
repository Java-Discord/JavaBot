package net.javadiscord.javabot.systems.qotw.model;

import lombok.Data;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.javadiscord.javabot.util.ExceptionLogger;

import java.util.function.Consumer;

/**
 * Simple data class that represents a single {@link ThreadChannel submission} and the corresponding
 * {@link User author}.
 */
@Data
public class QOTWSubmission {
	private final ThreadChannel thread;
	private User author;

	public boolean hasAuthor() {
		return author != null;
	}

	/**
	 * Attempts to retrieve the thread's actual author. Since the bot is creating submission threads, we can't use
	 * {@link ThreadChannel#getOwnerThreadMember()}, so we just filter all bot-users instead.
	 *
	 * @param onSuccess The success-{@link Consumer} for this operation.
	 */
	public void retrieveAuthor(Consumer<User> onSuccess) {
		if (author != null) {
			onSuccess.accept(author);
			return;
		}
		thread
			.getJDA()
			.retrieveUserById(thread.getName().split(" - ")[1])
			.queue(onSuccess, e -> ExceptionLogger.capture(e, QOTWSubmission.class.getSimpleName()));
	}
}
