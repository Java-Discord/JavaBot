package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.StatsCategory;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;
import java.util.Objects;

public class UserLeave extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {

        if (!new Database().getConfigBoolean(event.getGuild(), "other.server_lock.lock_status")) {
            var welcomeConfig = Bot.config.get(event.getGuild()).getWelcome();
            if (welcomeConfig.isEnabled()) {
                String leaveMessage = Objects.requireNonNull(welcomeConfig.getLeaveMessageTemplate());
                String replacedText;

                if (event.getUser().isBot()) {
                    List<Emote> Emote = event.getGuild().getEmotesByName("badgeBot", false);
                    replacedText = leaveMessage.replace("{!boticon}", " " + Emote.get(0).getAsMention());
                } else replacedText = leaveMessage.replace("{!boticon}", "");

                String replacedText2 = replacedText
                        .replace("{!member}", event.getUser().getAsMention())
                        .replace("{!membertag}", event.getUser().getAsTag())
                        .replace("{!server}", event.getGuild().getName());
                welcomeConfig.getChannel().sendMessage(replacedText2).queue();
            }
            StatsCategory.update(event.getGuild());
        }
    }
}
