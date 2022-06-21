package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

public final class Checks {
	public static final Pattern HEX_PATERN = Pattern.compile("^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$");

	private Checks() {
	}

	public static boolean checkLongInput(@NotNull OptionMapping mapping) {
		try {
			mapping.getAsLong();
			return true;
		} catch (IllegalStateException | NumberFormatException e) {
			return false;
		}
	}

	public static boolean checkGuild(@NotNull Interaction interaction) {
		return interaction.isFromGuild() && interaction.getGuild() != null;
	}

	public static boolean checkImageUrl(String url) {
		try {
			BufferedImage image = ImageIO.read(new URL(url));
			return image != null;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean checkHexColor(String hex) {
		return HEX_PATERN.matcher(hex).matches();
	}
}
