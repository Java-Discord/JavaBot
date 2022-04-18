package net.javadiscord.javabot.systems.qotw.subcommands.qotw_points;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.commands.LeaderboardCommand;
import net.javadiscord.javabot.systems.notification.QOTWNotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;

import java.time.Instant;

/**
 * Subcommand that allows staff-members to increment the QOTW-Account of any user.
 */
public class IncrementSubcommand implements SlashCommand {

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var userOption = event.getOption("user");
		if (userOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		Member member = userOption.getAsMember();
		DbHelper.doDaoAction(QuestionPointsRepository::new, dao -> {
			long points = dao.increment(member.getIdLong());
			MessageEmbed embed = this.buildIncrementEmbed(member, points);
			new GuildNotificationService(event.getGuild()).sendLogChannelNotification(embed);
			new QOTWNotificationService(member.getUser(), event.getGuild()).sendAccountIncrementedNotification();
			event.getHook().sendMessageEmbeds(embed).queue();
		});
		return event.deferReply();
	}

	private MessageEmbed buildIncrementEmbed(Member member, long points) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getUser().getEffectiveAvatarUrl())
				.setTitle("QOTW Account Incremented")
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getSuccessColor())
				.addField("Total QOTW-Points", "```" + points + "```", true)
				.addField("Rank", "```#" + LeaderboardCommand.getQOTWRank(member, member.getGuild()) + "```", true)
				.setFooter("ID: " + member.getId())
				.setTimestamp(Instant.now())
				.build();
	}
}
