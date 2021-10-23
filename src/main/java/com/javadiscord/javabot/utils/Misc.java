package com.javadiscord.javabot.utils;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;

import java.util.List;

public class Misc {

    public static void sendToLog(Guild guild, MessageEmbed embed) {
        Bot.config.get(guild).getModeration().getLogChannel().sendMessageEmbeds(embed).queue();
    }

    public static void sendToLog(Guild guild, String text) {
        Bot.config.get(guild).getModeration().getLogChannel().sendMessage(text).queue();
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

    public static String replaceTextVariables(Guild guild, String string) {
        return string
                .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                .replace("{!servername}", guild.getName())
                .replace("{!serverid}", guild.getId());
    }
}
