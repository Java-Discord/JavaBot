package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class LeaderboardCommand implements SlashCommandHandler {

	private final Color BACKGROUND_COLOR = Color.decode("#011E2F");
	private final Color PRIMARY_COLOR = Color.WHITE;
	private final Color SECONDARY_COLOR = Color.decode("#414A52");

	private final int LB_WIDTH = 3000;
	private final int CARD_HEIGHT = 350;
	private final int EMPTY_SPACE = 700;

	private final float NAME_SIZE = 65;
	private final float PLACEMENT_SIZE = 72;

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		OptionMapping option = event.getOption("amount");
		long l = option == null ? 10 : option.getAsLong();
		if (l > 30 || l < 2) return Responses.error(event, "```Please choose an amount between 2-30```");
		Bot.asyncPool.submit(() -> {
			event.getHook().sendFile(new ByteArrayInputStream(generateLeaderboard(event.getMember(), l).toByteArray()), "leaderboard" + ".png").queue();
		});

		return event.deferReply();
	}

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

	private List<Member> getTopUsers(Guild guild, int num) {
        try (var con = Bot.dataSource.getConnection()) {
            var repo = new QuestionPointsRepository(con);
            var accounts = repo.getAllAccountsSortedByPoints();
            return accounts.stream()
                    .map(QOTWAccount::getUserId).limit(num)
                    .map(guild::getMemberById).toList();
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
	}

	private BufferedImage getAvatar(String avatarURL) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(new URL(avatarURL));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return img;
	}

	private BufferedImage getImage(String resourcePath) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(Objects.requireNonNull(LeaderboardCommand.class.getClassLoader().getResourceAsStream(resourcePath)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return img;
	}

	private Font getFont(float size) {
		Font font;
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, LeaderboardCommand.class.getClassLoader().getResourceAsStream("fonts/Uni-Sans-Heavy.ttf")).deriveFont(size);
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
		} catch (IOException | FontFormatException e) {
			font = new Font("Arial", Font.PLAIN, (int) size);
		}
		return font;
	}

	private void drawUserCard(Graphics2D g2d, Member member, int yOffset, boolean drawLeft, boolean topten) {
		int xOffset = 200; // Left
		if (!drawLeft) xOffset = 1588; // Right
		else if (topten) xOffset = 894; // Center

		g2d.drawImage(getAvatar(member.getUser().getEffectiveAvatarUrl() + "?size=4096"), xOffset + 185, yOffset + 43, 200, 200, null);
		if (topten) g2d.drawImage(getImage("images/leaderboard/LBSelfCard.png"), xOffset, yOffset, null);
		else g2d.drawImage(getImage("images/leaderboard/LBCard.png"), xOffset, yOffset, null);

		g2d.setColor(PRIMARY_COLOR);
		g2d.setFont(getFont(NAME_SIZE));
		int stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getName());

		while (stringWidth > 750) {
			Font currentFont = g2d.getFont();
			Font newFont = currentFont.deriveFont(currentFont.getSize() - 1F);
			g2d.setFont(newFont);
			stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getName());
		}
		g2d.drawString(member.getUser().getName(), xOffset + 430, yOffset + 130);

		g2d.setColor(SECONDARY_COLOR);
		g2d.setFont(getFont(PLACEMENT_SIZE));

        try (var con = Bot.dataSource.getConnection()) {
            var repo = new QuestionPointsRepository(con);
            long points = repo.getAccountByUserId(member.getIdLong()).getPoints();
            String text = points > 1 ? "points" : "point";
            String rank = "#" + getQOTWRank(member.getIdLong());
            g2d.drawString(text, xOffset + 430, yOffset + 210);
            int stringLength = (int) g2d.getFontMetrics().getStringBounds(rank, g2d).getWidth();
            int start = 185 / 2 - stringLength / 2;
            g2d.drawString(rank, xOffset + start, yOffset + 173);
        } catch (SQLException e) {
            e.printStackTrace();
        }
	}

	private ByteArrayOutputStream generateLeaderboard(Member member, long num) {
		int LB_HEIGHT = (getTopUsers(member.getGuild(), (int) num).size() / 2) * CARD_HEIGHT + EMPTY_SPACE + 20;
		boolean topTen = getTopUsers(member.getGuild(), (int) num).contains(member);

		if (!topTen) LB_HEIGHT += CARD_HEIGHT;
		BufferedImage bufferedImage = new BufferedImage(LB_WIDTH, LB_HEIGHT, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = bufferedImage.createGraphics();

		g2d.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

		g2d.setRenderingHint(
				RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		g2d.setPaint(BACKGROUND_COLOR);
		g2d.fillRect(0, 0, LB_WIDTH, LB_HEIGHT);

		BufferedImage logo = getImage("images/leaderboard/Logo.png");
		g2d.drawImage(logo, LB_WIDTH / 2 - logo.getWidth() / 2, 110, null);

		int nameY = EMPTY_SPACE;
		boolean drawLeft = true;

		for (var m : getTopUsers(member.getGuild(), (int) num)) {
			drawUserCard(g2d, m, nameY, drawLeft, false);
			drawLeft = !drawLeft;
			if (drawLeft) nameY = nameY + CARD_HEIGHT;
		}

		if (!topTen) drawUserCard(g2d, member, nameY, true, true);
		g2d.dispose();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(bufferedImage, "png", outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return outputStream;
	}
}