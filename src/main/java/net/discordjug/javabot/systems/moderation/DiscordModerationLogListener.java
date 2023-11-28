package net.discordjug.javabot.systems.moderation;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.warn.dao.WarnRepository;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Logs moderative actions that did not use bot commands.
 */
@RequiredArgsConstructor
public class DiscordModerationLogListener extends ListenerAdapter{
	
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;

	@Override
	public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
		
		ModerationService moderationService = new ModerationService(notificationService, botConfig.get(event.getGuild()), warnRepository, asyncPool);
		
		AuditLogEntry entry = event.getEntry();
		long targetUserId = entry.getTargetIdLong();
		long moderatorUserId = entry.getUserIdLong();
		if (moderatorUserId == event.getJDA().getSelfUser().getIdLong()) {
			return;
		}
		event.getJDA().retrieveUserById(targetUserId).queue(targetUser -> {
			event.getGuild().retrieveMemberById(moderatorUserId).queue(moderator -> {
				String reason = entry.getReason();
				if (reason == null) {
					reason = "<no reason provided>";
				}
				switch(entry.getType()) {
				case KICK -> moderationService.sendKickGuildNotification(targetUser, reason, moderator);
				case BAN -> moderationService.sendBanGuildNotification(targetUser, reason, moderator);
				case UNBAN -> moderationService.sendUnbanGuildNotification(targetUser, reason, moderator);
				case MEMBER_UPDATE -> {
					if (entry.getChanges().containsKey("communication_disabled_until")) {
						AuditLogChange change = entry.getChangeByKey("communication_disabled_until");
						if (change.getNewValue()!=null) {
							ZonedDateTime timeoutRemovalTimestamp = java.time.ZonedDateTime.parse(change.getNewValue());
							moderationService.sendTimeoutGuildNotification(targetUser, reason, moderator, Duration.between(ZonedDateTime.now(), timeoutRemovalTimestamp));
						}else {
							moderationService.sendRemoveTimeoutGuildNotification(targetUser, reason, moderator);
						}
					}
				}
				default -> {}
				}
			});
		});
		
	}
	
}
