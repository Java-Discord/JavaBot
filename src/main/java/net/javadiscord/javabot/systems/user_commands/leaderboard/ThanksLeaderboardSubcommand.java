package net.javadiscord.javabot.systems.user_commands.leaderboard;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.util.Pair;
import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command that generates a leaderboard based on the help channel thanks count.
 */
public class ThanksLeaderboardSubcommand extends SlashCommand.Subcommand {
	public ThanksLeaderboardSubcommand() {
		setSubcommandData(new SubcommandData("thanks", "The Thanks Leaderboard."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.deferReply(false).queue();
		var collector = Collectors.joining("\n");
		var format = "**%d** %s";
		Bot.asyncPool.submit(() -> {
			var totalHelpers = getCounts("""
					SELECT COUNT(id), helper_id
					FROM help_channel_thanks
					GROUP BY helper_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.getSecond(), p.getFirst().getUser().getAsMention()))
					.collect(collector);
			var helpersThisWeek = getCounts("""
					SELECT COUNT(id), helper_id
					FROM help_channel_thanks
					WHERE thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))
					GROUP BY helper_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.getSecond(), p.getFirst().getUser().getAsMention()))
					.collect(collector);
			var totalHelped = getCounts("""
					SELECT COUNT(id) AS count, user_id
					FROM help_channel_thanks
					GROUP BY user_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.getSecond(), p.getFirst().getUser().getAsMention()))
					.collect(collector);
			var helpedThisWeek = getCounts("""
					SELECT COUNT(id) AS count, user_id
					FROM help_channel_thanks
					WHERE thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))
					GROUP BY user_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.getSecond(), p.getFirst().getUser().getAsMention()))
					.collect(collector);
			EmbedBuilder embed = new EmbedBuilder()
					.setTitle("Thanks Leaderboard")
					.setColor(Responses.Type.DEFAULT.getColor())
					.addField("Most Thanked This Week", helpersThisWeek, false)
					.addField("Most Thanked All Time", totalHelpers, false)
					.addField("Most Thankful This Week", helpedThisWeek, false)
					.addField("Most Thankful All Time", totalHelped, false);
			event.getHook().sendMessageEmbeds(embed.build()).queue();
		});
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
						memberData.sort((o1, o2) -> Long.compare(o2.getSecond(), o1.getSecond()));
						return memberData;
					}
			);
		} catch (SQLException e) {
			Sentry.captureException(e);
			return List.of();
		}
	}
}
