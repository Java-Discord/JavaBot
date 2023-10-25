package net.javadiscord.javabot.systems.user_commands.leaderboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.dv8tion.jda.api.entities.Member;
import net.javadiscord.javabot.util.ImageCache;
import net.javadiscord.javabot.util.ImageGenerationUtils;

/**
 * Creates graphical leaderboards.
 */
class LeaderboardCreator implements AutoCloseable{

	private static final Color PRIMARY_COLOR = Color.WHITE;
	private static final Color SECONDARY_COLOR = Color.decode("#414A52");
	private static final int MARGIN = 40;
	/**
	 * The image's width.
	 */
	private static final int WIDTH = 3000;

	private static final Color BACKGROUND_COLOR = Color.decode("#011E2F");
	private Graphics2D g2d;
	private int y;
	private boolean left;
	private BufferedImage image;

	/**
	 * Prepares drawing a leaderboard.
	 * @param numberOfEntries the number of entries in the leaderboard
	 * @param logoName the name of the logo put at the top of the leaderboard or {@code null} if no logo shall be used
	 * @throws IOException if anything goes wrong
	 */
	LeaderboardCreator(int numberOfEntries, String logoName) throws IOException{

		int logoHeight = 0;
		BufferedImage logo = null;
		if (logoName != null) {
			logo = ImageGenerationUtils.getResourceImage("assets/images/" + logoName + ".png");
			logoHeight = logo.getHeight();
		}

		int height = (logoHeight + MARGIN * 3) +
				(ImageGenerationUtils.getResourceImage("assets/images/LeaderboardUserCard.png").getHeight() + MARGIN) * ((int)Math.ceil(numberOfEntries / 2f)) + MARGIN;
		image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
		g2d = image.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setPaint(BACKGROUND_COLOR);
		g2d.fillRect(0, 0, WIDTH, height);
		if (logo != null) {
			g2d.drawImage(logo, WIDTH / 2 - logo.getWidth() / 2, MARGIN, null);
		}

		left = true;
		y = logoHeight + 3 * MARGIN;
	}

	/**
	 * adds a single entry in the leaderboard.
	 * @param member the {@link Member} this entry is responsible for or {@code null} if no member can be associated
	 * @param displayName the name to display
	 * @param points the amount of points
	 * @param rankNumber the rank of the given user
	 * @throws IOException if anything goes wrong
	 */
	public void drawLeaderboardEntry(@Nullable Member member, @NotNull String displayName, long points, int rankNumber) throws IOException {
		BufferedImage card = ImageGenerationUtils.getResourceImage("assets/images/LeaderboardUserCard.png");
		int x = left ? MARGIN * 5 : WIDTH - MARGIN * 5 - card.getWidth();
		if (member != null) {
			g2d.drawImage(ImageGenerationUtils.getImageFromUrl(member.getEffectiveAvatarUrl() + "?size=4096"), x + 185, y + 43, 200, 200, null);
		}
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

		String text = points + (points > 1 ? " points" : " point");
		String rank = "#" + rankNumber;
		g2d.drawString(text, x + 430, y + 210);
		int stringLength = (int) g2d.getFontMetrics().getStringBounds(rank, g2d).getWidth();
		int start = 185 / 2 - stringLength / 2;
		g2d.drawString(rank, x + start, y + 173);

		left = !left;
		if (left) y = y + card.getHeight() + MARGIN;
	}

	/**
	 * convert the drawn image to a {@code byte[]}.
	 *
	 * This also caches the image and invalidates all caches matching {@code invalidateCacheKeyword}
	 * @param cacheName the name of the cache where the image should be cached
	 * @param invalidateCacheKeyword all image caches containing this keyword will be invalidated, should be a substring of {@code cacheName}
	 * @return the drawn image as a {@code byte[]}
	 * @throws IOException if anything goes wrong
	 */
	public @NotNull byte[] getImageBytes(String cacheName, String invalidateCacheKeyword) throws IOException {
		ImageCache.removeCachedImagesByKeyword(invalidateCacheKeyword);
		ImageCache.cacheImage(cacheName, image);
		try (ByteArrayOutputStream baos = getOutputStreamFromImage(image)) {
			return baos.toByteArray();
		}
	}

	/**
	 * load an image from the cache as a {@code byte[]}.
	 * @param cacheName the name of the cache to load
	 * @param fallback a callback which is executed in case the cache could not be found
	 * @return the cached image or the fallback image
	 * @throws IOException if anything goes wrong
	 */
	public static byte[] attemptLoadFromCache(String cacheName, ByteArrayLoader fallback) throws IOException {
		return ImageCache.isCached(cacheName) ?
				// retrieve the image from the cache
				getOutputStreamFromImage(ImageCache.getCachedImage(cacheName)).toByteArray() :
				// generate an entirely new image
				fallback.load();
	}

	/**
	 * Retrieves the image's {@link ByteArrayOutputStream}.
	 *
	 * @param image The image.
	 * @return The image's {@link ByteArrayOutputStream}.
	 * @throws IOException If an error occurs.
	 */
	private static @NotNull ByteArrayOutputStream getOutputStreamFromImage(BufferedImage image) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return outputStream;
	}

	@Override
	public void close() {
		g2d.dispose();
	}

	@FunctionalInterface
	interface ByteArrayLoader{
		byte[] load() throws IOException;
	}

}
