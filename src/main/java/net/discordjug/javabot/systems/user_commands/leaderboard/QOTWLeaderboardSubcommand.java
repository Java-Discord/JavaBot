package net.discordjug.javabot.systems.user_commands.leaderboard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.discordjug.javabot.systems.qotw.model.QOTWAccount;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Pair;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

/**
 * Command for QOTW Leaderboard.
 */
public class QOTWLeaderboardSubcommand extends SlashCommand.Subcommand {

	private static final int DISPLAY_COUNT = 10;

	private final QOTWPointsService pointsService;
	private final ExecutorService asyncPool;
	private final QuestionPointsRepository qotwPointsRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param pointsService The {@link QOTWPointsService} managing {@link QOTWAccount}s
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param qotwPointsRepository Dao object that represents the QOTW_POINTS SQL Table.
	 */
	public QOTWLeaderboardSubcommand(QOTWPointsService pointsService, ExecutorService asyncPool, QuestionPointsRepository qotwPointsRepository) {
		setCommandData(new SubcommandData("qotw", "The QOTW Points Leaderboard."));
		this.pointsService=pointsService;
		this.asyncPool = asyncPool;
		this.qotwPointsRepository = qotwPointsRepository;
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		asyncPool.submit(() -> {
			try {
				WebhookMessageCreateAction<Message> action = event.getHook().sendMessageEmbeds(buildLeaderboardRankEmbed(event.getMember()));
				// check whether the image may already been cached
				byte[] array = LeaderboardCreator.attemptLoadFromCache(getCacheName(), ()->generateLeaderboard(event.getGuild()));
				action.addFiles(FileUpload.fromData(new ByteArrayInputStream(array), Instant.now().getEpochSecond() + ".png")).queue();
			} catch (IOException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
			}
		});
	}

	/**
	 * Builds the Leaderboard Rank {@link MessageEmbed}.
	 *
	 * @param member  The member which executed the command.
	 * @return A {@link MessageEmbed} object.
	 */
	private MessageEmbed buildLeaderboardRankEmbed(Member member) {
		int rank = pointsService.getQOTWRank(member.getIdLong());
		String rankSuffix = switch (rank % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
		long points = pointsService.getPoints(member.getIdLong());
		String pointsText = points == 1 ? "point" : "points";
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(member.getUser()), null, member.getEffectiveAvatarUrl())
				.setTitle("Question of the Week Leaderboard")
				.setDescription(points == 0 ? "You are currently not ranked." :
					String.format("This month, you're in `%s` place with `%s` %s.", rank + rankSuffix, points, pointsText))
				.setTimestamp(Instant.now())
				.build();
	}

	/**
	 * Draws a single "user card".
	 *
	 * @param leaderboardCreator handling actual drawing.
	 * @param member  The member.
	 * @param service The {@link QOTWPointsService}.
	 * @throws IOException If an error occurs.
	 */
	private void drawUserCard(LeaderboardCreator leaderboardCreator, @NotNull Member member, QOTWPointsService service) throws IOException {
		leaderboardCreator.drawLeaderboardEntry(member, UserUtils.getUserTag(member.getUser()), service.getPoints(member.getIdLong()), service.getQOTWRank(member.getIdLong()));
	}

	/**
	 * Draws and constructs the leaderboard image.
	 *
	 * @param guild   The current guild.
	 * @return The finished image as a {@link ByteArrayInputStream}.
	 * @throws IOException If an error occurs.
	 */
	private @NotNull byte[] generateLeaderboard(Guild guild) throws IOException {
		List<Pair<QOTWAccount, Member>> topMembers = pointsService.getTopMembers(DISPLAY_COUNT, guild);

		try(LeaderboardCreator creator = new LeaderboardCreator(Math.min(DISPLAY_COUNT, topMembers.size()), "QuestionOfTheWeekHeader")){
			for (Pair<QOTWAccount, Member> pair : topMembers) {
				drawUserCard(creator, pair.second(), pointsService);
			}
			return creator.getImageBytes(getCacheName(), "qotw_leaderboard");
		}
	}

	/**
	 * Builds the cached image's name.
	 *
	 * @return The image's cache name.
	 */
	private @NotNull String getCacheName() {
		try {
			List<QOTWAccount> accounts = qotwPointsRepository.sortByPoints(QOTWPointsService.getCurrentMonth())
					.stream()
					.limit(DISPLAY_COUNT)
					.toList();
			StringBuilder sb = new StringBuilder("qotw_leaderboard_");
			accounts.forEach(account -> sb.append(String.format(":%s:%s", account.getUserId(), account.getPoints())));
			return sb.toString();
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return "";
		}
	}
}
