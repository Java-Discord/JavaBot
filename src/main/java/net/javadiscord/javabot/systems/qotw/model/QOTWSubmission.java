package net.javadiscord.javabot.systems.qotw.model;

import lombok.Data;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.function.Consumer;

@Data
public class QOTWSubmission {
	private final ThreadChannel thread;
	private User author;

	public boolean hasAuthor() {
		return author != null;
	}

	public void retrieveAuthor(Consumer<User> onSuccess) {
		thread.retrieveThreadMembers().queue(s -> s.forEach(m -> {
			if (!hasAuthor() && !m.getUser().isBot()) {
				author = m.getUser();
				onSuccess.accept(author);
			}
		}));
	}
}
