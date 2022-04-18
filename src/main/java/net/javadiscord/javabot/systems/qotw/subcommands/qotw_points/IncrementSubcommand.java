package net.javadiscord.javabot.systems.qotw.subcommands.qotw_points;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
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
//TODO: refactor this whole thing
public class IncrementSubcommand implements SlashCommand {

	/**
	 * Increments the QOTW-Points of the given member by 1.
	 *
	 * @param member The member whose points should be incremented.
	 * @param quiet  If true, don't send a message in the channel.
	 * @return The new amount of QOTW-Points.
	 */
	public static long correct(User user, Guild guild, boolean quiet) {
		return correct(user, guild, quiet, false);
	}

	/**
	 * Increments the QOTW-Points of the given member by 1.
	 *
	 * @param member The member whose points should be incremented.
	 * @param quiet  If true, don't send a message in the channel.
	 * @param bestAnswer Whether it should send the Best Answer Embed instead.
	 * @return The new amount of QOTW-Points.
	 */
	public static long correct(User user, Guild guild, boolean quiet, boolean bestAnswer) {
		try (var con = Bot.dataSource.getConnection()) {
			QuestionPointsRepository repo = new QuestionPointsRepository(con);
			long points = repo.increment(user.getIdLong());
			MessageEmbed dmEmbed = bestAnswer ? buildBestAnswerDmEmbed(user, guild, points) : buildIncrementDmEmbed(user, guild, points);
			MessageEmbed embed = buildIncrementEmbed(user, guild, points);
			if (!quiet) GuildUtils.getLogChannel(guild).sendMessageEmbeds(embed).queue();
			user.openPrivateChannel().queue(
					c -> c.sendMessageEmbeds(dmEmbed).queue(),
					e -> GuildUtils.getLogChannel(guild).sendMessage("Could not send direct message to member " + user.getAsMention()).queue());
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
		User user = memberOption.getAsUser();
		long points = correct(user, false);
		MessageEmbed embed = buildIncrementEmbed(user, points);
		return event.replyEmbeds(embed);
	}

	private static MessageEmbed buildIncrementDmEmbed(User user, Guild guild, long points) {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was accepted! %s
								You've been granted **`1 QOTW-Point`**! (total: %s)""",
						Bot.config.get(guild).getEmote().getSuccessEmote().getAsMention(), points))
				.setTimestamp(Instant.now())
				.build();
	}

	private static MessageEmbed buildBestAnswerDmEmbed(User user, Guild guild, long points) {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was marked as the best answer!
								You've been granted **`1 extra QOTW-Point`**! (total: %s)""", points))
				.setTimestamp(Instant.now())
				.build();
	}

	private static MessageEmbed buildIncrementEmbed(User user, Guild guild, long points) {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag() + " | QOTW-Point added", null, user.getEffectiveAvatarUrl())
				.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
				.addField("Total QOTW-Points", "```" + points + "```", true)
				.addField("Rank", "```#" + LeaderboardCommand.getQOTWRank(user, guild) + "```", true)
				.setFooter("ID: " + user.getId())
				.setTimestamp(Instant.now())
				.build();
	}

}
