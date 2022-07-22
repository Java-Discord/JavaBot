package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class which contains some useful checks.
 */
public final class Checks {

	/**
	 * A {@link Pattern} that matches with a hex string (#FFFFFF).
	 */
	public static final Pattern HEX_PATTERN = Pattern.compile("^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$");

	private Checks() {
	}

	public static boolean hasPermissions(Guild guild, @NotNull Set<Permission> perms) {
		return perms.stream().allMatch(p -> hasPermission(guild, p));
	}

	public static boolean hasPermission(@NotNull Guild guild, Permission perm) {
		return guild.getSelfMember().hasPermission(perm);
	}

	/**
	 * Checks if the given {@link OptionMapping} is parsable to a {@link Long}.
	 *
	 * @param mapping The {@link OptionMapping} to check.
	 * @return Whether the {@link OptionMapping} is parsable to a {@link Long}.
	 */
	public static boolean isInvalidLongInput(@NotNull OptionMapping mapping) {
		try {
			mapping.getAsLong();
			return false;
		} catch (IllegalStateException | NumberFormatException e) {
			return true;
		}
	}

	/**
	 * Checks if the specified url points to a valid image.
	 *
	 * @param url The provided URL.
	 * @return Whether the specified url points to a valid image.
	 */
	public static boolean checkImageUrl(String url) {
		try {
			BufferedImage image = ImageIO.read(new URL(url));
			return image != null;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean checkHexColor(String hex) {
		return HEX_PATTERN.matcher(hex).matches();
	}
}
