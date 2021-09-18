package com.javadiscord.javabot.commands.reaction_roles;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class HelpRole  extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        if (event.getChannelType() == ChannelType.TEXT) {
            if (!event.getUser().isBot()) {
                long guildid = event.getGuild().getIdLong();
                long channelid = event.getChannel().getIdLong();
                long messageid = event.getMessageIdLong();
                String emote = "";
                if (guildid == 883199663891685446L && channelid == 883199663891685448L && messageid == 888696596303327263L) {
                    if (event.getReactionEmote().isEmoji()) {
                        emote = event.getReactionEmote().getEmoji();
                        System.out.println(emote);

                        if(emote.equals("✅")) {
                            Guild guild = event.getGuild();
                            guild.addRoleToMember(event.getMember(), guild.getRoleById(888699748159193089L)).queue();
                        }}

                }
            }
        }


    }
    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {

        if (event.getChannelType() == ChannelType.TEXT) {
            if (!event.getUser().isBot()) {
                long guildid = event.getGuild().getIdLong();
                long channelid = event.getChannel().getIdLong();
                long messageid = event.getMessageIdLong();
                String emote = "";
                if (guildid == 883199663891685446L && channelid == 883199663891685448L && messageid == 888696596303327263L ) {
                    if (event.getReactionEmote().isEmoji()) {
                        emote = event.getReactionEmote().getEmoji();
                        System.out.println(emote);

                        if(emote.equals("✅")) {
                            Guild guild = event.getGuild();
                            guild.removeRoleFromMember(event.getMember(), guild.getRoleById(888699748159193089L)).queue();
                        }}

                }
            }
        }


    }

}
