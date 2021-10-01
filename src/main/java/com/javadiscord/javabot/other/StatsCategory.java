package com.javadiscord.javabot.other;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.entities.Guild;

public class StatsCategory {

    public static void update(Guild guild) {
        var statsConfig = Bot.config.get(guild).getStats();
        String text = statsConfig.getMemberCountMessageTemplate()
                .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                .replace("{!server}", guild.getName());

        guild.getCategoryById(statsConfig.getCategoryId()).getManager().setName(text).queue();
    }
}
