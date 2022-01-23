package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.javadiscord.javabot.Bot;

import java.util.List;

/**
 * Utility class for various things.
 */
@Deprecated
public class Misc {

	private Misc() {}

	public static void sendToLog(Guild guild, MessageEmbed embed) {
		Bot.config.get(guild).getModeration().getLogChannel().sendMessageEmbeds(embed).queue();
	}

	public static void sendToLog(Guild guild, String text) {
		Bot.config.get(guild).getModeration().getLogChannel().sendMessage(text).queue();
	}

	/**
	 * Sends a message to the guild's log channel.
	 * @param guild The current guild.
	 * @param formatText The unformatted text.
	 * @param args The arguments.
	 */
	public static void sendToLogFormat(Guild guild, String formatText, Object... args) {
		Bot.config.get(guild).getModeration().getLogChannel().sendMessage(String.format(
				formatText,
				args
		)).queue();
	}

	/**
	 * Gets all guilds and formats them nicely.
	 * @param guildList A {@link List} with all guilds.
	 * @param showID Whether the guild's id should be appended every time.
	 * @param showMemCount Whether the guild's membercount should be appended every time.
	 * @return The formatted String.
	 */
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
	 * @param guild The current guild.
	 * @param string The string that should be replaced.
	 * @return The formatted String.
	 */
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
	public static String replaceTextVariables(Member member, String string) {
		return string
				.replace("{!membercount}", String.valueOf(member.getGuild().getMemberCount()))
				.replace("{!servername}", member.getGuild().getName())
				.replace("{!serverid}", member.getGuild().getId())
				.replace("{!member}", member.getAsMention())
				.replace("{!membertag}", member.getUser().getAsTag());
	}
}
