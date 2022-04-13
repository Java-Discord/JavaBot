package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.util.Pair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command that generates a leaderboard based on the help channel thanks count.
 */
public class ThanksLeaderboardCommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		var collector = Collectors.joining("\n");
		var format = "**%d** %s";
		Bot.asyncPool.submit(() -> {
			var totalHelpers = getCounts("""
					SELECT COUNT(id), helper_id
					FROM help_channel_thanks
					GROUP BY helper_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.second(), p.first().getUser().getAsMention()))
					.collect(collector);
			var helpersThisWeek = getCounts("""
					SELECT COUNT(id), helper_id
					FROM help_channel_thanks
					WHERE thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))
					GROUP BY helper_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.second(), p.first().getUser().getAsMention()))
					.collect(collector);
			var totalHelped = getCounts("""
					SELECT COUNT(id) AS count, user_id
					FROM help_channel_thanks
					GROUP BY user_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.second(), p.first().getUser().getAsMention()))
					.collect(collector);
			var helpedThisWeek = getCounts("""
					SELECT COUNT(id) AS count, user_id
					FROM help_channel_thanks
					WHERE thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))
					GROUP BY user_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.second(), p.first().getUser().getAsMention()))
					.collect(collector);
			EmbedBuilder embed = new EmbedBuilder()
					.setTitle("Thanks Leaderboard")
					.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
					.addField("Most Thanked This Week", helpersThisWeek, false)
					.addField("Most Thanked All Time", totalHelpers, false)
					.addField("Most Thankful This Week", helpedThisWeek, false)
					.addField("Most Thankful All Time", totalHelped, false);
			event.getHook().sendMessageEmbeds(embed.build()).queue();
		});
		return event.deferReply(false);
	}

	private List<Pair<Member, Long>> getCounts(String query, Guild guild) {
		try {
			return DbActions.mapQuery(
					query,
					s -> {
					},
					rs -> {
						List<Pair<Member, Long>> memberData = new ArrayList<>();
						while (rs.next()) {
							long count = rs.getLong(1);
							long userId = rs.getLong(2);
							var member = guild.getMemberById(userId);
							if (member == null) continue;
							memberData.add(new Pair<>(member, count));
						}
						// Sort with high counts first.
						memberData.sort((o1, o2) -> Long.compare(o2.second(), o1.second()));
						return memberData;
					}
			);
		} catch (SQLException e) {
			e.printStackTrace();
			return List.of();
		}
	}
}
