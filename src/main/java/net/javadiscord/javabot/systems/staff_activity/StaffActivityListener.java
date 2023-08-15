package net.javadiscord.javabot.systems.staff_activity;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.systems.staff_activity.dao.StaffActivityMessageRepository;
import net.javadiscord.javabot.systems.staff_activity.model.StaffActivityMessage;
import net.javadiscord.javabot.util.ExceptionLogger;

/**
 * Listener for tracking staff activity.
 * This class maintains a message for each staff member in each channel.
 * Each time the staff member sends a message, the message associated with the staff member is updated.
 */
@RequiredArgsConstructor
public class StaffActivityListener extends ListenerAdapter {
	
	private final BotConfig botConfig;
	private final StaffActivityMessageRepository repository;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			return;
		}
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
			return;
		}
		ModerationConfig moderationConfig = botConfig.get(event.getGuild()).getModerationConfig();
		TextChannel staffActivityChannel = moderationConfig.getStaffActivityChannel();
		if (staffActivityChannel == null) {
			return;
		}
		Member member = event.getMember();
		if (!member.getRoles().contains(moderationConfig.getStaffRole())) {
			return;
		}
		
		sendStaffActivityEmbed(event, staffActivityChannel, member);
	}

	private void sendStaffActivityEmbed(MessageReceivedEvent event, TextChannel staffActivityChannel, Member member) {
		MessageEmbed embed = new EmbedBuilder()
			.setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl())
			.setDescription("Last sent message: " + TimeFormat.RELATIVE.format(event.getMessage().getTimeCreated()))
			.setFooter(member.getId())
			.build();
		
		Long msgId = repository.getMessageId(staffActivityChannel.getGuild().getIdLong(), member.getIdLong());
		if (msgId != null) {
			staffActivityChannel
				.retrieveMessageById(msgId)
				.queue(
						activityMessage -> activityMessage.editMessageEmbeds(embed).queue(),
						notFound -> createNewMessage(staffActivityChannel, embed, member));
		} else {
			createNewMessage(staffActivityChannel, embed, member);
		}
	}

	private void createNewMessage(TextChannel staffActivityChannel, MessageEmbed embed, Member member) {
		staffActivityChannel.sendMessageEmbeds(embed).queue(success -> 
			repository.insertOrReplace(new StaffActivityMessage(member.getGuild().getIdLong(), member.getIdLong(), success.getIdLong())),
			error -> ExceptionLogger.capture(error, "Cannot create new staff activity message")
		);
	}
}
