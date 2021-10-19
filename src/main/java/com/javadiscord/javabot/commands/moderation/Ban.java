package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.utils.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;

public class Ban implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        Member member = event.getOption("user").getAsMember();
        if (member == null) {
            return Responses.error(event, "Cannot kick a user who is not a member of this server");
        }

        var eb = banEmbed(member, event.getMember(), event.getGuild(), reason);
        try {
            ban(member, reason);
            Misc.sendToLog(event.getGuild(), eb);
            if (member.getUser().hasPrivateChannel()) {
                member.getUser().openPrivateChannel().complete().
                        sendMessageEmbeds(eb).queue();
            }
            return event.replyEmbeds(eb);
        }
        catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }
    }

    public void ban(Member member, String reason) {
        new Warn().deleteAllDocs(member.getId());
        member.ban(6, reason).queue();
    }

    public MessageEmbed banEmbed(Member member, Member mod, Guild guild, String reason) {
        return new EmbedBuilder()
                .setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
                .setAuthor(member.getUser().getAsTag() + " | Ban", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", member.getUser().getAsMention(), true)
                .addField("Moderator", mod.getAsMention(), true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter(mod.getUser().getAsTag(), mod.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
    }

    public void handleBanInteraction(Member member, ButtonClickEvent event) {
        if (member == null) {
            Responses.error(event.getHook(), "Couldn't find member").queue();
            return;
        }

        event.getHook().editOriginalComponents()
                .setActionRows(
                        ActionRow.of(
                                Button.danger("utils:ban", "Banned " + member.getUser().getAsTag()).asDisabled())
                )
                .queue();

        var eb = new Ban().banEmbed(member, event.getMember(), event.getGuild(), "None");
        new Ban().ban(member, "None");

        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();
        event.replyEmbeds(eb);
    }
}