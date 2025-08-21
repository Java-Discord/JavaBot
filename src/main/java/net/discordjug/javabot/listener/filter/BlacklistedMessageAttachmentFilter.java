package net.discordjug.javabot.listener.filter;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This {@link MessageFilter} blocks attachments blacklisted using {@link GuildConfig#getBlacklistedMessageExtensions()}.
 */
@Component
@RequiredArgsConstructor
public class BlacklistedMessageAttachmentFilter implements MessageFilter {

	private final BotConfig botConfig;

	@Override
	public MessageModificationStatus processMessage(MessageContent content) {
		MessageReceivedEvent event = content.event();
		List<Message.Attachment> attachments = content.attachments();
		List<MessageEmbed> embeds = content.embeds();
		GuildConfig guildConfig = botConfig.get(event.getGuild());
		List<String> blacklistedMessageExtensions = guildConfig.getBlacklistedMessageExtensions();
		boolean removed = attachments.removeIf(attachment -> blacklistedMessageExtensions.contains(attachment.getFileExtension()));
		if (removed) {
			MessageEmbed attachmentRemovedInfo = new EmbedBuilder()
					.setDescription("Disallowed attachments have been removed from this message.")
					.build();
			embeds.add(attachmentRemovedInfo);
			return MessageModificationStatus.MODIFIED;
		} else {
			return MessageModificationStatus.NOT_MODIFIED;
		}
	}
}
