package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.util.TimeUtils;

import java.time.Instant;

public class ServerInfoCommand implements SlashCommandHandler {

	@Override
	public ReplyAction handle(SlashCommandEvent event) {

		if (event.getGuild() == null) return Responses.warning(event, "This can only be used in a guild.");
		long roleCount = (long) event.getGuild().getRoles().size() - 1;
		long catCount = event.getGuild().getCategories().size();
		long textChannelCount = event.getGuild().getTextChannels().size();
		long voiceChannelCount = event.getGuild().getVoiceChannels().size();
		long channelCount = event.getGuild().getChannels().size() - catCount;

		String guildDate = event.getGuild().getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);
		String createdDiff = " (" + new TimeUtils().formatDurationToNow(event.getGuild().getTimeCreated()) + ")";

		EmbedBuilder eb = new EmbedBuilder()
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setThumbnail(event.getGuild().getIconUrl())
				.setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
				.addField("Name", "```" + event.getGuild().getName() + "```", true)
				.addField("Owner", "```" + event.getGuild().getOwner().getUser().getAsTag() + "```", true)
				.addField("ID", "```" + event.getGuild().getId() + "```", false)
				.addField("Roles", "```" + roleCount + " Roles```", true)
				.addField("Channel Count", "```" + channelCount + " Channel, " + catCount + " Categories" +
						"\n → Text: " + textChannelCount +
						"\n → Voice: " + voiceChannelCount + "```", false)

				.addField("Member Count", "```" + event.getGuild().getMemberCount() + " members```", false)
				.addField("Server created on", "```" + guildDate + createdDiff + "```", false)
				.setTimestamp(Instant.now());

		if (event.getGuild().getId().equals("648956210850299986")) {
			return event.replyEmbeds(eb.build()).addActionRow(Button.link(Constants.WEBSITE_LINK, "Website"));
		} else {
			return event.replyEmbeds(eb.build());
		}
	}
}
