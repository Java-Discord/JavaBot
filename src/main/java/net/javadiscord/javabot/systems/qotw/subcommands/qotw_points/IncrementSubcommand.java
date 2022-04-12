package net.javadiscord.javabot.systems.qotw.subcommands.qotw_points;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.systems.commands.LeaderboardCommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.util.GuildUtils;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Subcommand that allows staff-members to increment the QOTW-Account of any user.
 */
public class IncrementSubcommand implements SlashCommand {

	/**
	 * Increments the QOTW-Points of the given member by 1.
	 *
	 * @param member The member whose points should be incremented.
	 * @param quiet  If true, don't send a message in the channel.
	 * @return The new amount of QOTW-Points.
	 */
	public static long correct(Member member, boolean quiet) {
		return correct(member, quiet, false);
	}

	/**
	 * Increments the QOTW-Points of the given member by 1.
	 *
	 * @param member The member whose points should be incremented.
	 * @param quiet  If true, don't send a message in the channel.
	 * @param bestAnswer Whether it should send the Best Answer Embed instead.
	 * @return The new amount of QOTW-Points.
	 */
	public static long correct(Member member, boolean quiet, boolean bestAnswer) {
		try (var con = Bot.dataSource.getConnection()) {
			QuestionPointsRepository repo = new QuestionPointsRepository(con);
			long memberId = member.getIdLong();
			repo.increment(memberId);
			long points = repo.getAccountByUserId(memberId).getPoints();
			MessageEmbed dmEmbed = bestAnswer ? buildBestAnswerDmEmbed(member, points) : buildIncrementDmEmbed(member, points);
			MessageEmbed embed = buildIncrementEmbed(member, points);
			if (!quiet) GuildUtils.getLogChannel(member.getGuild()).sendMessageEmbeds(embed).queue();
			member.getUser().openPrivateChannel().queue(
					c -> c.sendMessageEmbeds(dmEmbed).queue(),
					e -> GuildUtils.getLogChannel(member.getGuild()).sendMessage("Could not send direct message to member " + member.getAsMention()).queue());
			return points;
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var memberOption = event.getOption("user");
		if (memberOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		Member member = memberOption.getAsMember();
		long points = correct(member, false);
		MessageEmbed embed = buildIncrementEmbed(member, points);
		return event.replyEmbeds(embed);
	}

	private static MessageEmbed buildIncrementDmEmbed(Member member, long points) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getUser().getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was accepted! %s
								You've been granted **`1 QOTW-Point`**! (total: %s)""",
						Bot.config.get(member.getGuild()).getEmote().getSuccessEmote().getAsMention(), points))
				.setTimestamp(Instant.now())
				.build();
	}

	private static MessageEmbed buildBestAnswerDmEmbed(Member member, long points) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getUser().getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was marked as the best answer!
								You've been granted **`1 extra QOTW-Point`**! (total: %s)""", points))
				.setTimestamp(Instant.now())
				.build();
	}

	private static MessageEmbed buildIncrementEmbed(Member member, long points) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag() + " | QOTW-Point added", null, member.getUser().getEffectiveAvatarUrl())
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getSuccessColor())
				.addField("Total QOTW-Points", "```" + points + "```", true)
				.addField("Rank", "```#" + new LeaderboardCommand().getQOTWRank(member, member.getGuild()) + "```", true)
				.setFooter("ID: " + member.getId())
				.setTimestamp(Instant.now())
				.build();
	}

}
