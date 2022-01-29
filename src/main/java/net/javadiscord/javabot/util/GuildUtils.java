package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.javadiscord.javabot.Bot;

import java.util.List;

/**
 * Utility class for various things.
 */
public class GuildUtils {

	private GuildUtils() {
	}

	public static MessageChannel getLogChannel(Guild guild) {
		return Bot.config.get(guild).getModeration().getLogChannel();
	}

	/**
	 * Gets all guilds and formats them nicely.
	 *
	 * @param guildList    A {@link List} with all guilds.
	 * @param showID       Whether the guild's id should be appended every time.
	 * @param showMemCount Whether the guild's membercount should be appended every time.
	 * @return The formatted String.
	 */
	@Deprecated
	public static String getGuildList(List<Guild> guildList, boolean showID, boolean showMemCount) {
		StringBuilder sb = new StringBuilder();
		for (int guildAmount = guildList.size(); guildAmount > 0; guildAmount--) {

			sb.append(", ").append(guildList.get(guildAmount - 1).getName());

			if (showID && showMemCount) {
				sb.append(" (").append(guildList.get(guildAmount - 1).getId()).append(", ").append(guildList.get(guildAmount - 1).getMemberCount()).append(" members)");
			} else if (showID && !showMemCount) {
				sb.append(" (").append(guildList.get(guildAmount - 1).getId()).append(")");
			} else if (!showID && showMemCount) {
				sb.append(" (").append(guildList.get(guildAmount - 1).getMemberCount()).append(" members)");
			}
		}
		return sb.substring(2);
	}

	/**
	 * Utility method that replaces text variables.
	 *
	 * @param guild  The current guild.
	 * @param string The string that should be replaced.
	 * @return The formatted String.
	 */
	@Deprecated
	public static String replaceTextVariables(Guild guild, String string) {
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
	public static String replaceTextVariables(Member member, String string) {
		return string
				.replace("{!membercount}", String.valueOf(member.getGuild().getMemberCount()))
				.replace("{!servername}", member.getGuild().getName())
				.replace("{!serverid}", member.getGuild().getId())
				.replace("{!member}", member.getAsMention())
				.replace("{!membertag}", member.getUser().getAsTag());
	}
}
