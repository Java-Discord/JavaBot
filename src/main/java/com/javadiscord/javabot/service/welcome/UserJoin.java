package com.javadiscord.javabot.service.welcome;

import com.javadiscord.javabot.service.serverlock.ServerLock;
import com.javadiscord.javabot.utils.Misc;
import com.javadiscord.javabot.utils.TimeUtils;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class UserJoin extends ListenerAdapter {

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getUser().isBot()) return;

        ServerLock lock = new ServerLock();

        if (lock.lockStatus(event.getGuild())) {
            event.getUser().openPrivateChannel().queue(c -> {
                c.sendMessage("https://discord.gg/java")
                    .setEmbeds(ServerLock.lockEmbed(event.getGuild())).queue();
                event.getMember().kick().queue();
            });

            String diff = new TimeUtils().formatDurationToNow(event.getMember().getTimeCreated());
            Misc.sendToLog(event.getGuild(), String.format("**%s** (%s old) tried to join this server.",
                    event.getMember().getUser().getAsTag(), diff));
        } else {
            lock.checkLock(event, event.getUser());
        }
    }
}



