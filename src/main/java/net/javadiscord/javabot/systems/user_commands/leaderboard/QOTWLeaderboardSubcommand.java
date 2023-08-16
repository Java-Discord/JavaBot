package net.javadiscord.javabot.systems.user_commands.leaderboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.imageio.ImageIO;

import net.javadiscord.javabot.util.UserUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.ImageCache;
import net.javadiscord.javabot.util.ImageGenerationUtils;
import net.javadiscord.javabot.util.Pair;

/**
 * Command for QOTW Leaderboard.
 */
public class QOTWLeaderboardSubcommand extends SlashCommand.Subcommand {
	private static final Color BACKGROUND_COLOR = Color.decode("#011E2F");
	private static final Color PRIMARY_COLOR = Color.WHITE;
	private static final Color SECONDARY_COLOR = Color.decode("#414A52");
	private static final int DISPLAY_COUNT = 10;
	private static final int MARGIN = 40;

	/**
	 * The image's width.
	 */
	private static final int WIDTH = 3000;

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
				byte[] array = ImageCache.isCached(getCacheName()) ?
						// retrieve the image from the cache
						getOutputStreamFromImage(ImageCache.getCachedImage(getCacheName())).toByteArray() :
						// generate an entirely new image
						generateLeaderboard(event.getGuild()).toByteArray();
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
	 * Draws a single "user card" at the given coordinates.
	 *
	 * @param g2d     Graphics object.
	 * @param member  The member.
	 * @param service The {@link QOTWPointsService}.
	 * @param y       The y-position.
	 * @param left    Whether the card should be drawn left or right.
	 * @throws IOException If an error occurs.
	 */
	private void drawUserCard(@NotNull Graphics2D g2d, @NotNull Member member, QOTWPointsService service, int y, boolean left) throws IOException {
		BufferedImage card = ImageGenerationUtils.getResourceImage("assets/images/LeaderboardUserCard.png");
		int x = left ? MARGIN * 5 : WIDTH - MARGIN * 5 - card.getWidth();
		g2d.drawImage(ImageGenerationUtils.getImageFromUrl(member.getEffectiveAvatarUrl() + "?size=4096"), x + 185, y + 43, 200, 200, null);
		String displayName = UserUtils.getUserTag(member.getUser());
		// draw card
		g2d.drawImage(card, x, y, null);
		g2d.setColor(PRIMARY_COLOR);
		g2d.setFont(ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", 65).orElseThrow());

		int stringWidth = g2d.getFontMetrics().stringWidth(displayName);
		while (stringWidth > 750) {
			Font currentFont = g2d.getFont();
			Font newFont = currentFont.deriveFont(currentFont.getSize() - 1F);
			g2d.setFont(newFont);
			stringWidth = g2d.getFontMetrics().stringWidth(displayName);
		}
		g2d.drawString(displayName, x + 430, y + 130);
		g2d.setColor(SECONDARY_COLOR);
		g2d.setFont(ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", 72).orElseThrow());

		long points = service.getPoints(member.getIdLong());
		String text = points + (points > 1 ? " points" : " point");
		String rank = "#" + service.getQOTWRank(member.getIdLong());
		g2d.drawString(text, x + 430, y + 210);
		int stringLength = (int) g2d.getFontMetrics().getStringBounds(rank, g2d).getWidth();
		int start = 185 / 2 - stringLength / 2;
		g2d.drawString(rank, x + start, y + 173);
	}

	/**
	 * Draws and constructs the leaderboard image.
	 *
	 * @param guild   The current guild.
	 * @return The finished image as a {@link ByteArrayInputStream}.
	 * @throws IOException If an error occurs.
	 */
	private @NotNull ByteArrayOutputStream generateLeaderboard(Guild guild) throws IOException {
		BufferedImage logo = ImageGenerationUtils.getResourceImage("assets/images/QuestionOfTheWeekHeader.png");
		BufferedImage card = ImageGenerationUtils.getResourceImage("assets/images/LeaderboardUserCard.png");

		List<Pair<QOTWAccount, Member>> topMembers = pointsService.getTopMembers(DISPLAY_COUNT, guild);
		int height = (logo.getHeight() + MARGIN * 3) +
				(ImageGenerationUtils.getResourceImage("assets/images/LeaderboardUserCard.png").getHeight() + MARGIN) * ((int)Math.ceil(Math.min(DISPLAY_COUNT, topMembers.size()) / 2f)) + MARGIN;
		BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		try {
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
			g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2d.setPaint(BACKGROUND_COLOR);
			g2d.fillRect(0, 0, WIDTH, height);
			g2d.drawImage(logo, WIDTH / 2 - logo.getWidth() / 2, MARGIN, null);

			boolean left = true;
			int y = logo.getHeight() + 3 * MARGIN;
			for (Pair<QOTWAccount, Member> pair : topMembers) {
				drawUserCard(g2d, pair.second(), pointsService, y, left);
				left = !left;
				if (left) y = y + card.getHeight() + MARGIN;
			}
			ImageCache.removeCachedImagesByKeyword("qotw_leaderboard");
			ImageCache.cacheImage(getCacheName(), image);
			return getOutputStreamFromImage(image);
		} finally {
			g2d.dispose();
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

	/**
	 * Retrieves the image's {@link ByteArrayOutputStream}.
	 *
	 * @param image The image.
	 * @return The image's {@link ByteArrayOutputStream}.
	 * @throws IOException If an error occurs.
	 */
	private @NotNull ByteArrayOutputStream getOutputStreamFromImage(BufferedImage image) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return outputStream;
	}
}
