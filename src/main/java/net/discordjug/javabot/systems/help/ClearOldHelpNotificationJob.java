package net.discordjug.javabot.systems.help;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import net.dv8tion.jda.api.utils.FileUpload;

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
			foundSoFar.addAll(
				msgs
					.stream()
					.filter(msg -> msg.getAuthor().getIdLong() == msg.getJDA().getSelfUser().getIdLong())
					.filter(msg -> msg.getTimeCreated().isBefore(OffsetDateTime.now().minusDays(3)))
					.filter(msg -> msg
						.getButtons()
						.stream()
						.anyMatch(button -> "Mark as unacknowledged".equals(button.getLabel())))
					.toList()
			);
			if (!msgs.isEmpty()) {
				deleteOldMessagesInChannel(helpNotificationChannel, history, foundSoFar);
			}else {
				if (foundSoFar.size() > 50) {
					String messageInfo = foundSoFar
							.stream()
							.map(msg -> convertMessageToString(msg))
							.collect(Collectors.joining("\n\n===============\n\n"));
					botConfig.get(helpNotificationChannel.getGuild()).getModerationConfig().getLogChannel()
						.sendMessageFormat("Warning: deleting %d messages in help notification channel, this might be due to help notification spam", foundSoFar.size())
						.addFiles(FileUpload.fromData(messageInfo.getBytes(StandardCharsets.UTF_8), "messages.txt"))
						.queue();
				}
				helpNotificationChannel.purgeMessages(foundSoFar);
			}
		}, e -> {
			ExceptionLogger.capture(e, getClass().getName());
			helpNotificationChannel.purgeMessages(foundSoFar);
		});
	}

	private String convertMessageToString(Message msg) {
		return msg.getContentRaw()+"\n"+
		msg.getEmbeds().stream().map(e->e.toData().toString()).collect(Collectors.joining("\n"));
	}
}
