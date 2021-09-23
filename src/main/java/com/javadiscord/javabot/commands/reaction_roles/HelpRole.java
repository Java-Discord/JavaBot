package com.javadiscord.javabot.commands.reaction_roles;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.properties.config.guild.ModerationConfig;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;






/** Listener class for the reaction role system .
 * @author Snape25
 */

public class HelpRole  extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getChannelType() == ChannelType.TEXT) {
            if (!event.getUser().isBot()) {
                String emote = "";

                /** Get required values from
                 * @see ModerationConfig
                 */
                TextChannel helpChannel = Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesChannel();
                Long MessageId = Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesMessageId();
                Role HelpRole =Bot.config.get(event.getGuild()).getModeration().getVerifiedHelpRole();
                String RequiredEmoji =Bot.config.get(event.getGuild()).getModeration().getHelpRoleImoji();

                if (event.getChannel().equals(helpChannel) && event.getMessageIdLong() == MessageId) {
                    if (event.getReactionEmote().isEmoji()) {
                        emote = event.getReactionEmote().getEmoji();
                        System.out.println(emote);
                        if (emote.equals(RequiredEmoji)) {
                            Guild guild = event.getGuild();
                            guild.addRoleToMember(event.getMember(), HelpRole).queue();
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

                /** Get required values from
                 * @see ModerationConfig
                 */

                TextChannel helpChannel = Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesChannel();
                Long MessageId = Bot.config.get(event.getGuild()).getModeration().getHelpGuidelinesMessageId();
                Role HelpRole =Bot.config.get(event.getGuild()).getModeration().getVerifiedHelpRole();
                String RequiredEmoji =Bot.config.get(event.getGuild()).getModeration().getHelpRoleImoji();

                if ( event.getChannel().equals(helpChannel) && event.getMessageIdLong() == MessageId ) {
                    if (event.getReactionEmote().isEmoji()) {
                        emote = event.getReactionEmote().getEmoji();
                        System.out.println(emote);
                        if(emote.equals(emote.equals(RequiredEmoji))) {
                            Guild guild = event.getGuild();
                            guild.removeRoleFromMember(event.getMember(), HelpRole).queue();
                        }
                    }
                }
            }
        }
    }


}
