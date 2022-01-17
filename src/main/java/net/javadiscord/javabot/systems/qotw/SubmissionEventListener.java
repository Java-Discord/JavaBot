package net.javadiscord.javabot.systems.qotw;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.thread.member.ThreadMemberLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SubmissionEventListener extends ListenerAdapter {
	@Override
	public void onThreadMemberLeave(@NotNull ThreadMemberLeaveEvent event) {
		var thread = event.getThread();
		if (thread.getName().contains(event.getMember().getId())) {
			thread.delete().queue();
			log.info("Deleted {}'s Submission Thread", event.getMember().getUser().getAsTag());
		}
	}
}
