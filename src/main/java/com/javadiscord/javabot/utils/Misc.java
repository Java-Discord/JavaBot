package com.javadiscord.javabot.utils;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public class Misc {

    public static void sendToLog(Guild guild, MessageEmbed embed) {
        Bot.config.get(guild).getModeration().getLogChannel().sendMessageEmbeds(embed).queue();
    }

    public static void sendToLog(Guild guild, String text) {
        Bot.config.get(guild).getModeration().getLogChannel().sendMessage(text).queue();
    }

    public static void sendToLogFormat(Guild guild, String formatText, Object... args) {
        Bot.config.get(guild).getModeration().getLogChannel().sendMessage(String.format(
                formatText,
                args
        )).queue();
    }

    public static String getGuildList (List<Guild> guildList, boolean showID, boolean showMemCount) {
        StringBuilder sb = new StringBuilder();
        for (int guildAmount = guildList.size(); guildAmount > 0; guildAmount--) {

            sb.append(", ").append(guildList.get(guildAmount - 1).getName());

                    if (showID && showMemCount) sb.append(" (").append(guildList.get(guildAmount - 1).getId()).append(", ").append(guildList.get(guildAmount - 1).getMemberCount()).append(" members)");
                    else if (showID && !showMemCount) sb.append(" (").append(guildList.get(guildAmount - 1).getId()).append(")");
                    else if (!showID && showMemCount) sb.append(" (").append(guildList.get(guildAmount - 1).getMemberCount()).append(" members)");
        }
        return sb.substring(2);
    }

    /**
     * Utility method that replaces text variables
     * @param string The string that should be replaced
     */
    public static String replaceTextVariables(Guild guild, String string) {
        return string
                .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                .replace("{!servername}", guild.getName())
                .replace("{!serverid}", guild.getId());
    }

    /**
     * Utility method that replaces text variables
     * @param string The string that should be replaced
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
