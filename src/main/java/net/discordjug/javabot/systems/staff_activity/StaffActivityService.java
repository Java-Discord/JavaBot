package net.discordjug.javabot.systems.staff_activity;

import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_activity.dao.StaffActivityMessageRepository;
import net.discordjug.javabot.systems.staff_activity.model.StaffActivityMessage;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.TimeFormat;

/**
 * Responsible for staff activity tracking.
 * This class maintains a message for each staff member in each channel.
 * This message is updated when a staff activity action occurs.
 */
@Service
@RequiredArgsConstructor
public class StaffActivityService {
	private final BotConfig botConfig;
	private final StaffActivityMessageRepository repository;
	
	/**
	 * Updates the staff activity message or creates it if necessary.
	 * Called when a tracked staff activity is executed.
	 * If no channel is configured for staff activity, this method doesn't do anything.
	 * @param type The type of the occured staff activity
	 * @param timestamp The timestamp when the activity occured
	 * @param member The saff member
	 */
	public void updateStaffActivity(StaffActivityType type, TemporalAccessor timestamp, Member member) {
		TextChannel staffActivityChannel = botConfig.get(member.getGuild()).getModerationConfig().getStaffActivityChannel();
		if (staffActivityChannel == null) {
			return;
		}
		Long msgId = repository.getMessageId(staffActivityChannel.getGuild().getIdLong(), member.getIdLong());
		if (msgId != null) {
			staffActivityChannel
				.retrieveMessageById(msgId)
				.queue(
						activityMessage -> replaceActivityMessage(type, timestamp, activityMessage, member),
						notFound -> createNewMessage(staffActivityChannel, member, type, timestamp));
		} else {
			createNewMessage(staffActivityChannel, member, type, timestamp);
		}
	}
	
	private void replaceActivityMessage(StaffActivityType type, TemporalAccessor timestamp, Message activityMessage, Member member) {
		List<MessageEmbed> embeds = activityMessage.getEmbeds();
		MessageEmbed embed;
		if(embeds.isEmpty()) {
			embed = createStaffActivityEmbedWithEntry(member, type, timestamp);
		}else {
			embed = replaceActivityEmbedField(embeds.get(0), type, timestamp);
		}
		activityMessage.editMessageEmbeds(embed).queue();
	}

	private MessageEmbed replaceActivityEmbedField(MessageEmbed embed, StaffActivityType type, TemporalAccessor timestamp) {
		EmbedBuilder eb = new EmbedBuilder(embed);
		for (Iterator<Field> it = eb.getFields().iterator(); it.hasNext();) {
			Field field = it.next();
			if(type.getTitle().equals(field.getName())) {
				it.remove();
			}
		}
		eb.addField(type.getTitle(), TimeFormat.RELATIVE.format(timestamp), false);
		return eb.build();
	}

	void createNewMessage(TextChannel staffActivityChannel, Member member, StaffActivityType type, TemporalAccessor timestamp) {
		MessageEmbed embed = createStaffActivityEmbedWithEntry(member, type, timestamp);
		staffActivityChannel.sendMessageEmbeds(embed).queue(success -> 
			repository.insertOrReplace(new StaffActivityMessage(member.getGuild().getIdLong(), member.getIdLong(), success.getIdLong())),
			error -> ExceptionLogger.capture(error, "Cannot create new staff activity message")
		);
	}

	private MessageEmbed createStaffActivityEmbedWithEntry(Member member, StaffActivityType type, TemporalAccessor timestamp) {
		return replaceActivityEmbedField(createEmptyStaffActivityEmbed(member), type, timestamp);
	}
	
	private MessageEmbed createEmptyStaffActivityEmbed(Member member) {
		return new EmbedBuilder()
				.setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl())
				.setFooter(member.getId())
				.build();
	}
}
