package com.javadiscord.javabot.other;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class StatsCategory {

    public static void update (Object ev) {

        Guild guild = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            guild = event.getGuild();
        }

        if (ev instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent event = (GuildMemberJoinEvent) ev;

            guild = event.getGuild();
        }

        if (ev instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent event = (GuildMemberRemoveEvent) ev;

            guild = event.getGuild();
        }

        Object event = ev;

        String text = Database.getConfigString(event, "other.stats_category.stats_text")
                .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                .replace("{!server}", guild.getName());

        guild.getCategoryById(Database.getConfigString(event, "other.stats_category.stats_cid")).getManager().setName(text).queue();
    }
}
