package com.javadiscord.javabot.other;

import net.dv8tion.jda.api.entities.Guild;

public class StatsCategory {

    public static void update (Guild guild) {

        String text = new Database().getConfigString(guild, "other.stats_category.stats_text")
                .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                .replace("{!server}", guild.getName());

        guild.getCategoryById(new Database().getConfigString(guild, "other.stats_category.stats_cid")).getManager().setName(text).queue();
    }
}
