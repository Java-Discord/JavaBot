package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
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
            unreserveAllChannels(event.getUser(), event.getGuild());
        }
    }

    /**
     * Unreserves any help channels that a leaving user may have reserved.
     * @param user The user who is leaving.
     * @param guild The guild they're leaving.
     */
    private void unreserveAllChannels(User user, Guild guild) {
        try (var con = Bot.dataSource.getConnection()) {
            var stmt = con.prepareStatement("SELECT channel_id FROM reserved_help_channels WHERE user_id = ?");
            stmt.setLong(1, user.getIdLong());
            var rs = stmt.getResultSet();
            var manager = new HelpChannelManager(Bot.config.get(guild).getHelp());
            while (rs.next()) {
                long channelId = rs.getLong("channel_id");
                manager.unreserveChannel(guild.getTextChannelById(channelId)).queue();
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            var logChannel = Bot.config.get(guild).getModeration().getLogChannel();
            logChannel.sendMessage("Database error while unreserving channels for a user who left: " + e.getMessage()).queue();
        }
    }
}
