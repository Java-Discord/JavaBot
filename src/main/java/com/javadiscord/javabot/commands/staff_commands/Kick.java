package com.javadiscord.javabot.commands.staff_commands;

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
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;

public class Kick implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        Member member = event.getOption("user").getAsMember();
        if (member == null) {
            return Responses.error(event, "Cannot kick a user who is not a member of this server");
        }

        var eb = kickEmbed(member, event.getMember(), event.getGuild(), reason);
        try {
            kick(member, reason);
            Misc.sendToLog(event.getGuild(), eb);
            member.getUser().openPrivateChannel().queue(m -> m.sendMessageEmbeds(eb).queue());
            return event.replyEmbeds(eb);
        } catch (Exception e) {
            return Responses.error(event, e.getMessage()); }
    }

    public void kick(Member member, String reason) {
        new Warn().deleteAllDocs(member.getId());
        member.kick(reason).queue();
    }

    /**
     * Returns a kick embed
     * @param member The member that should be kicked
     * @param mod The member that kicked the user
     * @param guild The current guild
     * @param reason The reason why the member was kicked
     */
    public MessageEmbed kickEmbed(Member member, Member mod, Guild guild, String reason) {
        return new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Kick", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
                .addField("Member", member.getAsMention(), true)
                .addField("Moderator", mod.getAsMention(), true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter(mod.getUser().getAsTag(), mod.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
    }

    /**
     * Handles an interaction, that should kick a member from the current guild.
     * @param member The member that should be kicked
     * @param event The ButtonClickEvent, that is triggered upon use.
     */
    public RestAction<?> handleKickInteraction(Member member, ButtonClickEvent event) {
        if (member == null) {
            event.getHook().editOriginalComponents().setActionRows(ActionRow.of(
                    Button.secondary("dummy-button", "Couldn't find Member").asDisabled())
            ).queue();
            return Responses.error(event.getHook(), "Couldn't find member");
        }
        event.getHook().editOriginalComponents()
                .setActionRows(
                        ActionRow.of(
                                Button.danger("dummy-button", "Kicked " + member.getUser().getAsTag()).asDisabled())
                ).queue();

        var eb = new Kick().kickEmbed(member, event.getMember(), event.getGuild(), "None");
        try {
            new Kick().kick(member, "None");
        } catch (Exception e) {
            return Responses.error(event.getHook(), e.getMessage());
        }

        member.getUser().openPrivateChannel().queue(m -> m.sendMessageEmbeds(eb).queue());
        return Bot.config.get(event.getGuild()).getModeration().getLogChannel().sendMessageEmbeds(eb);
    }
}