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
import net.discordjug.javabot.systems.qotw.model.QOTWAccount;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Pair;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
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

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param pointsService The {@link QOTWPointsService} managing {@link QOTWAccount}s
	 * @param asyncPool The thread pool for asynchronous operations
	 */
	public QOTWLeaderboardSubcommand(QOTWPointsService pointsService, ExecutorService asyncPool) {
		setCommandData(new SubcommandData("qotw", "The QOTW Points Leaderboard."));
		this.pointsService=pointsService;
		this.asyncPool = asyncPool;
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		asyncPool.submit(() -> {
			try {
				List<Pair<QOTWAccount,Member>> topMembers = pointsService.getTopMembers(DISPLAY_COUNT, event.getGuild());
				WebhookMessageCreateAction<Message> action = event.getHook().sendMessageEmbeds(buildLeaderboardRankEmbed(event.getMember(), topMembers));
				// check whether the image may already been cached
				byte[] array = LeaderboardCreator.attemptLoadFromCache(getCacheName(topMembers), ()->generateLeaderboard(topMembers));
				action.addFiles(FileUpload.fromData(new ByteArrayInputStream(array), Instant.now().getEpochSecond() + ".png")).queue();
			} catch (IOException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
			}
		});
	}

	/**
	 * Builds the Leaderboard Rank {@link MessageEmbed}.
	 *
	 * @param topMembers the accounts with the top QOTW users
	 * @param member  The member which executed the command.
	 * @return A {@link MessageEmbed} object.
	 */
	private MessageEmbed buildLeaderboardRankEmbed(Member member, List<Pair<QOTWAccount, Member>> topMembers) {
		int rank = findRankOfMember(member, topMembers);
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

	private int findRankOfMember(Member member, List<Pair<QOTWAccount, Member>> topMembers) {
		return pointsService.getQOTWRank(member.getIdLong(), 
				topMembers
				.stream()
				.map(Pair::first)
				.toList());
	}

	/**
	 * Draws a single "user card".
	 *
	 * @param leaderboardCreator handling actual drawing.
	 * @param member  The member.
	 * @param service The {@link QOTWPointsService}.
	 * @param topMembers the accounts with the top QOTW users
	 * @throws IOException If an error occurs.
	 */
	private void drawUserCard(LeaderboardCreator leaderboardCreator, @NotNull Member member, QOTWPointsService service, List<Pair<QOTWAccount, Member>> topMembers) throws IOException {
		leaderboardCreator.drawLeaderboardEntry(member, UserUtils.getUserTag(member.getUser()), service.getPoints(member.getIdLong()), findRankOfMember(member, topMembers));
	}

	/**
	 * Draws and constructs the leaderboard image.
	 *
	 * @param topMembers the accounts with the top QOTW users
	 * @return The finished image as a {@link ByteArrayInputStream}.
	 * @throws IOException If an error occurs.
	 */
	private @NotNull byte[] generateLeaderboard(List<Pair<QOTWAccount, Member>> topMembers) throws IOException {
		try(LeaderboardCreator creator = new LeaderboardCreator(Math.min(DISPLAY_COUNT, topMembers.size()), "QuestionOfTheWeekHeader")){
			for (Pair<QOTWAccount, Member> pair : topMembers) {
				drawUserCard(creator, pair.second(), pointsService, topMembers);
			}
			return creator.getImageBytes(getCacheName(topMembers), "qotw_leaderboard");
		}
	}

	/**
	 * Builds the cached image's name.
	 *
	 * @param topMembers the accounts with the top QOTW users
	 * @return The image's cache name.
	 */
	private @NotNull String getCacheName(List<Pair<QOTWAccount, Member>> topMembers) {
		try {
			StringBuilder sb = new StringBuilder("qotw_leaderboard_");
			topMembers.forEach(account -> sb.append(String.format(":%s:%s", account.first().getUserId(), account.first().getPoints())));
			return sb.toString();
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return "";
		}
	}
}
