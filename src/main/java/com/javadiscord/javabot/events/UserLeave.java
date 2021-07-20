package com.javadiscord.javabot.events;

import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.StatsCategory;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class UserLeave extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getMember().getUser().isBot()) return;

        if (!Database.getConfigBoolean(event, "other.server_lock.lock_status")) {
            if (Database.getConfigBoolean(event, "welcome_system.welcome_status")) {

                String leaveMessage = Database.getConfigString(event, "welcome_system.leave_msg");
                String replacedText;

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

                event.getGuild().getTextChannelById(String.valueOf(Database.getConfigString(event, "welcome_system.welcome_cid"))).sendMessage(replacedText2).queue();
            }

            StatsCategory.update(event.getGuild());
        }
    }
}
