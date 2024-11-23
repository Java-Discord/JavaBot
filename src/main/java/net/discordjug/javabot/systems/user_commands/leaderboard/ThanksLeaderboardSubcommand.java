package net.discordjug.javabot.systems.user_commands.leaderboard;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.util.Pair;
import net.discordjug.javabot.data.h2db.DbActions;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Command that generates a leaderboard based on the help channel thanks count.
 */
public class ThanksLeaderboardSubcommand extends SlashCommand.Subcommand {

	private final ExecutorService asyncPool;
	private final DbActions dbActions;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param dbActions A service object responsible for various operations on the main database
	 */
	public ThanksLeaderboardSubcommand(ExecutorService asyncPool, DbActions dbActions) {
		this.asyncPool = asyncPool;
		this.dbActions = dbActions;
		setCommandData(new SubcommandData("thanks", "The Thanks Leaderboard."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.deferReply(false).queue();
		Collector<CharSequence, ?, String> collector = Collectors.joining("\n");
		String format = "**%d** %s";
		asyncPool.submit(() -> {
			String totalHelpers = getCounts("""
					SELECT COUNT(id), helper_id
					FROM help_channel_thanks
					GROUP BY helper_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.getSecond(), p.getFirst().getUser().getAsMention()))
					.collect(collector);
			String helpersThisWeek = getCounts("""
					SELECT COUNT(id), helper_id
					FROM help_channel_thanks
					WHERE thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))
					GROUP BY helper_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.getSecond(), p.getFirst().getUser().getAsMention()))
					.collect(collector);
			String totalHelped = getCounts("""
					SELECT COUNT(id) AS count, user_id
					FROM help_channel_thanks
					GROUP BY user_id""", event.getGuild()).stream()
					.limit(3)
					.map(p -> String.format(format, p.getSecond(), p.getFirst().getUser().getAsMention()))
					.collect(collector);
			String helpedThisWeek = getCounts("""
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
			return dbActions.mapQuery(
					query,
					s -> {
					},
					rs -> {
						List<Pair<Member, Long>> memberData = new ArrayList<>();
						while (rs.next()) {
							long count = rs.getLong(1);
							long userId = rs.getLong(2);
							Member member = guild.retrieveMemberById(userId).complete();
							if (member == null) continue;
							memberData.add(new Pair<>(member, count));
						}
						// Sort with high counts first.
						memberData.sort((o1, o2) -> Long.compare(o2.getSecond(), o1.getSecond()));
						return memberData;
					}
			);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return List.of();
		}
	}
}
