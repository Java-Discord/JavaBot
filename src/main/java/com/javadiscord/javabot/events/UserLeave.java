package com.javadiscord.javabot.events;

import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class UserLeave extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {


        if ((Database.getConfigString(event, "lock")).equalsIgnoreCase("false")) {
            String leaveMessage = Database.getConfigString(event, "leave_msg");
            String replacedText;
            Guild guild = event.getGuild();

            if (event.getUser().isBot()) {
                List<Emote> Emote = event.getGuild().getEmotesByName("badgeBot", false);
                replacedText = leaveMessage.replace("{!boticon}", " " + Emote.get(0).getAsMention());
            } else {
                replacedText = leaveMessage.replace("{!boticon}", "");
            }

            String replacedText2 = replacedText
                    .replace("{!member}", event.getUser().getAsMention())
                    .replace("{!membertag}", event.getUser().getAsTag())
                    .replace("{!server}", event.getGuild().getName());

            event.getGuild().getTextChannelById(String.valueOf(Database.getConfigString(event, "welcome_cid"))).sendMessage(replacedText2).queue();

            String text = Database.getConfigString(event, "stats_msg")
                    .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                    .replace("{!server}", guild.getName());

            guild.getCategoryById(Database.getConfigString(event, "stats_cid")).getManager().setName(text).queue();
        }
    }
}
