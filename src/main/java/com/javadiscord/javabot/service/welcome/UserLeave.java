package com.javadiscord.javabot.service.welcome;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.service.help.HelpChannelManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.SQLException;

public class UserLeave extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getUser().isBot()) return;

        if (!Bot.config.get(event.getGuild()).getServerLock().isLocked()) {
            unreserveAllChannels(event.getUser(), event.getGuild());
        }
    }

    /**
     * Unreserves any help channels that a leaving user may have reserved.
     * @param user The user who is leaving.
     * @param guild The guild they're leaving.
     */
    private void unreserveAllChannels(User user, Guild guild) {
        try (var con = Bot.dataSource.getConnection();
                var stmt = con.prepareStatement("SELECT channel_id FROM reserved_help_channels WHERE user_id = ?")) {
            stmt.setLong(1, user.getIdLong());
            var rs = stmt.getResultSet();
            var manager = new HelpChannelManager(Bot.config.get(guild).getHelp());
            if (rs != null) {
                while (rs.next()) {
                    long channelId = rs.getLong("channel_id");
                    manager.unreserveChannel(guild.getTextChannelById(channelId)).queue();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            var logChannel = Bot.config.get(guild).getModeration().getLogChannel();
            logChannel.sendMessage("Database error while unreserving channels for a user who left: " + e.getMessage()).queue();
        }
    }
}
