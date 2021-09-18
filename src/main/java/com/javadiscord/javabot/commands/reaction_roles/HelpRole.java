package com.javadiscord.javabot.commands.reaction_roles;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.properties.config.guild.ModerationConfig;
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
                String emote = "";
                if (event.getChannel().equals(Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesChannel()) && event.getMessageIdLong() == Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesMessageId()) {
                    if (event.getReactionEmote().isEmoji()) {
                        emote = event.getReactionEmote().getEmoji();
                        System.out.println(emote);
                        if (emote.equals(Bot.config.get(event.getGuild()).getModeration().getHelpRoleImoji())) {
                            Guild guild = event.getGuild();
                            guild.addRoleToMember(event.getMember(), guild.getRoleById(888699748159193089L)).queue();
                        }
                    }
                }
            }
        }
    }


    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (event.getChannelType() == ChannelType.TEXT) {
            if (!event.getUser().isBot()) {
                String emote = "";
                if ( event.getChannel().equals(Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesChannel()) && event.getMessageIdLong() == Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesMessageId() ) {
                    if (event.getReactionEmote().isEmoji()) {
                        emote = event.getReactionEmote().getEmoji();
                        System.out.println(emote);
                        if(emote.equals(emote.equals(Bot.config.get(event.getGuild()).getModeration().getHelpRoleImoji()))) {
                            Guild guild = event.getGuild();
                            guild.removeRoleFromMember(event.getMember(), guild.getRoleById(888699748159193089L)).queue();
                        }
                    }
                }
            }
        }
    }


}
