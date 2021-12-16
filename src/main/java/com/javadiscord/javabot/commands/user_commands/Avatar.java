package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

/**
 * Command for displaying a full-size version of a user's avatar.
 */
public class Avatar implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping option = event.getOption("user");
        Member member = option == null ? event.getMember() : option.getAsMember();
        if (member == null) {
            return Responses.warning(event, "Sorry, this command can only be used in servers.");
        }
        return event.replyEmbeds(generateAvatarEmbed(member.getGuild(), member.getEffectiveName(), member.getEffectiveAvatarUrl()));
    }

    private MessageEmbed generateAvatarEmbed(Guild guild, String tag, String avatarUrl) {
        return new EmbedBuilder()
                .setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
                .setAuthor(tag + " | Avatar")
                .setImage(avatarUrl + "?size=4096")
                .build();
    }
}
