package com.javadiscord.javabot.other;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class StatsCategory {

    public static void update (Guild guild) {

        String text = Database.getConfigString(guild, "other.stats_category.stats_text")
                .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                .replace("{!server}", guild.getName());

        guild.getCategoryById(Database.getConfigString(guild, "other.stats_category.stats_cid")).getManager().setName(text).queue();
    }
}
