package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;


/**
 * Utility class for various things.
 */
@Deprecated(forRemoval = true)
public class GuildUtils {

	private GuildUtils() {
	}

	/**
	 * Utility method that replaces text variables.
	 *
	 * @param guild  The current guild.
	 * @param string The string that should be replaced.
	 * @return The formatted String.
	 */
	@Deprecated
	public static @NotNull String replaceTextVariables(@NotNull Guild guild, @NotNull String string) {
		return string
				.replace("{!membercount}", String.valueOf(guild.getMemberCount()))
				.replace("{!servername}", guild.getName())
				.replace("{!serverid}", guild.getId());
	}

	/**
	 * Utility method that replaces text variables.
	 *
	 * @param member The member object.
	 * @param string The string that should be replaced.
	 * @return The formatted String.
	 */
	@Deprecated
	public static @NotNull String replaceTextVariables(@NotNull Member member, @NotNull String string) {
		return string
				.replace("{!membercount}", String.valueOf(member.getGuild().getMemberCount()))
				.replace("{!servername}", member.getGuild().getName())
				.replace("{!serverid}", member.getGuild().getId())
				.replace("{!member}", member.getAsMention())
				.replace("{!membertag}", member.getUser().getAsTag());
	}
}
