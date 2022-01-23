package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;

import java.time.Instant;

/**
 * Command that allows members to report other members.
 */
public class ReportCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		OptionMapping option = event.getOption("reason");
		String reason = option == null ? "None" : option.getAsString();
		Member member = event.getOption("user").getAsMember();
		if (member == null) {
			return Responses.error(event, "Cannot report a user who is not a member of this server");
		}
		var config = Bot.config.get(event.getGuild());
		MessageChannel reportChannel = config.getModeration().getReportChannel();
		var embed = buildReportEmbed(member.getUser(), event.getUser(), reason, event.getTextChannel(), config.getSlashCommand());
		reportChannel.sendMessage("@here").setEmbeds(embed.build())
				.setActionRow(
						Button.danger("utils:ban:" + member.getId(), "Ban"),
						Button.danger("utils:kick:" + member.getId(), "Kick"),
						Button.secondary("utils:delete", "🗑️")
				)
				.queue();
		embed.setDescription("Successfully reported " + "`" + member.getUser().getAsTag() + "`!\nYour report has been send to our Moderators");
		return event.replyEmbeds(embed.build()).setEphemeral(true);
	}

	private EmbedBuilder buildReportEmbed(User reported, User reportedBy, String reason, TextChannel channel, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(reported.getAsTag(), null, reported.getEffectiveAvatarUrl())
				.setTitle("Report")
				.setColor(config.getDefaultColor())
				.addField("Member", reported.getAsMention(), true)
				.addField("Reported by", reportedBy.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Reported on", String.format("<t:%s:F>", Instant.now().getEpochSecond()), false)
				.addField("ID", String.format("```%s```", reported.getId()), true)
				.addField("Reason", String.format("```%s```", reason), false)
				.setFooter(reportedBy.getAsTag(), reportedBy.getEffectiveAvatarUrl())
				.setTimestamp(Instant.now());
	}
}


