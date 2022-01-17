package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.events.thread.member.ThreadMemberLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import org.jetbrains.annotations.NotNull;

public class SubmissionEventListener extends ListenerAdapter {
	@Override
	public void onThreadMemberLeave(@NotNull ThreadMemberLeaveEvent event) {
		var config = Bot.config.get(event.getGuild());
		var manager = new SubmissionManager(config.getQotw());
		var threads = manager.getSubmissionThreads(event.getMember());
		if (!threads.isEmpty()) {
			threads.stream()
					.filter(c -> c.equals(event.getThread()))
					.findFirst()
					.ifPresent(b -> b.delete().queue());
		}
	}
}