package net.discordjug.javabot.systems.help;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Automatically delete old help notifications.
 * Once a day, this class deletes old messages of the bot in the help notification channel.
 */
@Service
@RequiredArgsConstructor
public class ClearOldHelpNotificationJob {
	private final BotConfig botConfig;
	private final JDA jda;
	
	/**
	 * Runs the message deletion.
	 */
	@Scheduled(cron="0 0 0 * * *")//00:00 UTC
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			TextChannel helpNotificationChannel = botConfig.get(guild).getHelpConfig().getHelpNotificationChannel();
			if(helpNotificationChannel != null) {
				MessageHistory history = helpNotificationChannel.getHistory();
				deleteOldMessagesInChannel(helpNotificationChannel, history, new ArrayList<>());
			}
		}
	}

	private void deleteOldMessagesInChannel(TextChannel helpNotificationChannel, MessageHistory history, List<Message> foundSoFar) {
		history.retrievePast(50).queue(msgs -> {
			boolean deleteMore = addMessagesToDelete(foundSoFar, msgs);
			if (deleteMore) {
				deleteOldMessagesInChannel(helpNotificationChannel, history, foundSoFar);
			}else {
				helpNotificationChannel.purgeMessages(foundSoFar);
			}
		}, e -> {
			ExceptionLogger.capture(e, getClass().getName());
			helpNotificationChannel.purgeMessages(foundSoFar);
		});
	}

	private boolean addMessagesToDelete(List<Message> toDelete, List<Message> msgs) {
		for (Message message : msgs) {
			if (message.getAuthor().getIdLong() == message.getJDA().getSelfUser().getIdLong() &&
					//only delete messages older than 5 days
					message.getTimeCreated().isBefore(OffsetDateTime.now().minusDays(5))) {
				
				//only messages sent within the past two weeks can be deleted
				//stop when messages near that are found
				if (message.getTimeCreated().isBefore(OffsetDateTime.now().minusDays(13))) {
					return false;
				}
				toDelete.add(message);
			}
		}
		return true;
	}
}
