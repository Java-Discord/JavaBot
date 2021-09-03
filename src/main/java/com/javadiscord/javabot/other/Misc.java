package com.javadiscord.javabot.other;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.imageio.ImageIO;
import java.net.URL;
import java.util.List;

import static com.javadiscord.javabot.events.Startup.iae;

public class Misc {

    public static String checkImage (String input) {

        try {
            ImageIO.read(new URL(input));
        } catch (Exception e) {
            input = iae;
        }

        return input;
    }

    public static boolean isImage (String input) {

        boolean b = true;

        try {
            ImageIO.read(new URL(input));
        } catch (Exception e) {
            b = false;
        }

        return b;
    }

    public static void sendToLog(Guild guild, MessageEmbed embed) {

        guild.getTextChannelById(new Database().getConfigString(guild, "channels.log_cid")).sendMessageEmbeds(embed).queue();
    }

    public static void sendToLog(Guild guild, String text) {

        guild.getTextChannelById(new Database().getConfigString(guild, "channels.log_cid")).sendMessage(text).queue();
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
}
