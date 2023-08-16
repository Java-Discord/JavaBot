package net.javadiscord.javabot.systems.staff_activity;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;

/**
 * Listener for tracking staff activity.
 * Each time the staff member sends a message, the staff activity message is updated.
 * @see StaffActivityService
 */
@RequiredArgsConstructor
public class StaffActivityListener extends ListenerAdapter {
	
	private final BotConfig botConfig;
	private final StaffActivityService service;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			return;
		}
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
			return;
		}
		ModerationConfig moderationConfig = botConfig.get(event.getGuild()).getModerationConfig();
		Member member = event.getMember();
		if (!member.getRoles().contains(moderationConfig.getStaffRole())) {
			return;
		}
		
		service.updateStaffActivity(StaffActivityType.LAST_MESSAGE, event.getMessage().getTimeCreated(), member);
	}
}
