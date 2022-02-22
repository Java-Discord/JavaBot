package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.util.ImageGenerationUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static net.javadiscord.javabot.Bot.imageCache;

/**
 * Command that generates a leaderboard based on QOTW-Points.
 */
public class LeaderboardCommand extends ImageGenerationUtils implements ISlashCommand {

	private final Color BACKGROUND_COLOR = Color.decode("#011E2F");
	private final Color PRIMARY_COLOR = Color.WHITE;
	private final Color SECONDARY_COLOR = Color.decode("#414A52");

	private final int DISPLAY_COUNT = 10;

	private final int MARGIN = 40;
	private final int WIDTH = 3000;

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		Bot.asyncPool.submit(() -> {
			try {
				var action = event.getHook().sendMessageEmbeds(buildLeaderboardRankEmbed(event.getMember()));
				byte[] array;
				if (imageCache.isCached(getCacheName())) {
					array = getOutputStreamFromImage(imageCache.getCachedImage(getCacheName())).toByteArray();
				} else {
					array = generateLeaderboard(event.getGuild()).toByteArray();
				}
				action.addFile(new ByteArrayInputStream(array), Instant.now().getEpochSecond() + ".png").queue();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		return event.deferReply();
	}

	/**
	 * Gets the given user's QOTW-Rank.
	 *
	 * @param userId The id of the user.
	 * @return The QOTW-Rank as an integer.
	 */
	public int getQOTWRank(long userId) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QuestionPointsRepository(con);
			var accounts = repo.getAllAccountsSortedByPoints();
			return accounts.stream().map(QOTWAccount::getUserId).toList().indexOf(userId) + 1;
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Gets the top N members based on their QOTW-Points.
	 *
	 * @param n     The amount of members to get.
	 * @param guild The current guild.
	 * @return A {@link List} with the top member ids.
	 */
	private List<Member> getTopNMembers(int n, Guild guild) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QuestionPointsRepository(con);
			var accounts = repo.getAllAccountsSortedByPoints();
			return accounts.stream()
					.map(QOTWAccount::getUserId)
					.map(guild::getMemberById)
					.filter(Objects::nonNull)
					.limit(n)
					.toList();
		} catch (SQLException e) {
			e.printStackTrace();
			return List.of();
		}
	}

	/**
	 * Gets the given user's QOTW-Points.
	 *
	 * @param userId The id of the user.
	 * @return The user's total QOTW-Points
	 */
	private long getPoints(long userId) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QuestionPointsRepository(con);
			return repo.getAccountByUserId(userId).getPoints();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Builds the Leaderboard Rank {@link MessageEmbed}.
	 *
	 * @param member The member which executed the command.
	 * @return A {@link MessageEmbed} object.
	 */
	private MessageEmbed buildLeaderboardRankEmbed(Member member) {
		var rank = getQOTWRank(member.getIdLong());
		var rankSuffix = switch (rank % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
		var points = getPoints(member.getIdLong());
		var pointsText = points == 1 ? "point" : "points";
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setTitle("Question of the Week Leaderboard")
				.setDescription(String.format("You're currently in `%s` place with `%s` %s.",
						rank + rankSuffix, points, pointsText))
				.setTimestamp(Instant.now())
				.build();
	}

	/**
	 * Draws a single "user card" at the given coordinates.
	 *
	 * @param g2d    Graphics object.
	 * @param guild  The current Guild.
	 * @param member The member.
	 * @param y      The y-position.
	 * @param left   Whether the card should be drawn left or right.
	 * @throws IOException If an error occurs.
	 */
	private void drawUserCard(Graphics2D g2d, Guild guild, Member member, int y, boolean left) throws IOException {
		var card = getResourceImage("images/leaderboard/LBCard.png");
		int x;
		if (left) {
			x = MARGIN * 5;
		} else {
			x = WIDTH - (MARGIN * 5) - card.getWidth();
		}


		g2d.drawImage(getImageFromUrl(member.getUser().getEffectiveAvatarUrl() + "?size=4096"), x + 185, y + 43, 200, 200, null);
		var displayName = member.getUser().getAsTag();
		g2d.drawImage(card, x, y, null);
		g2d.setColor(PRIMARY_COLOR);
		g2d.setFont(getResourceFont("fonts/Uni-Sans-Heavy.ttf", 65).orElseThrow());

		int stringWidth = g2d.getFontMetrics().stringWidth(displayName);
		while (stringWidth > 750) {
			var currentFont = g2d.getFont();
			var newFont = currentFont.deriveFont(currentFont.getSize() - 1F);
			g2d.setFont(newFont);
			stringWidth = g2d.getFontMetrics().stringWidth(displayName);
		}
		g2d.drawString(displayName, x + 430, y + 130);
		g2d.setColor(SECONDARY_COLOR);
		g2d.setFont(getResourceFont("fonts/Uni-Sans-Heavy.ttf", 72).orElseThrow());

		var points = getPoints(member.getIdLong());
		String text = points + (points > 1 ? " points" : " point");
		String rank = "#" + getQOTWRank(member.getIdLong());
		g2d.drawString(text, x + 430, y + 210);
		int stringLength = (int) g2d.getFontMetrics().getStringBounds(rank, g2d).getWidth();
		int start = 185 / 2 - stringLength / 2;
		g2d.drawString(rank, x + start, y + 173);
	}

	/**
	 * Draws and constructs the leaderboard image.
	 *
	 * @param guild The current guild.
	 * @return The finished image as a {@link ByteArrayInputStream}.
	 * @throws IOException If an error occurs.
	 */
	private ByteArrayOutputStream generateLeaderboard(Guild guild) throws IOException {
		var logo = getResourceImage("images/leaderboard/Logo.png");
		var card = getResourceImage("images/leaderboard/LBCard.png");

		var topMembers = getTopNMembers(DISPLAY_COUNT, guild);
		int height = (logo.getHeight() + MARGIN * 3) +
				(getResourceImage("images/leaderboard/LBCard.png").getHeight() + MARGIN) * (Math.min(DISPLAY_COUNT, topMembers.size()) / 2) + MARGIN;
		var image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setPaint(BACKGROUND_COLOR);
		g2d.fillRect(0, 0, WIDTH, height);
		g2d.drawImage(logo, WIDTH / 2 - logo.getWidth() / 2, MARGIN, null);

		boolean left = true;
		int y = logo.getHeight() + 3 * MARGIN;
		for (var m : topMembers) {
			drawUserCard(g2d, guild, m, y, left);
			left = !left;
			if (left) y = y + card.getHeight() + MARGIN;
		}
		g2d.dispose();
		imageCache.removeCachedImagesByKeyword("qotw_leaderboard");
		imageCache.cacheImage(getCacheName(), image);
		return getOutputStreamFromImage(image);
	}

	/**
	 * Builds the cached image's name.
	 *
	 * @return The image's cache name.
	 */
	private String getCacheName() {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QuestionPointsRepository(con);
			var accounts = repo.getAllAccountsSortedByPoints().stream().limit(DISPLAY_COUNT).toList();
			StringBuilder sb = new StringBuilder("qotw_leaderboard_");
			for (var a : accounts) {
				sb.append(":").append(a.getUserId())
						.append(":").append(a.getPoints());
			}
			return sb.toString();
		} catch (SQLException e) {
			e.printStackTrace();
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
	private ByteArrayOutputStream getOutputStreamFromImage(BufferedImage image) throws IOException {
		var outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return outputStream;
	}
}