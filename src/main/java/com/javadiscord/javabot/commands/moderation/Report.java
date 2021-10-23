package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;

public class Report implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        Member member = event.getOption("user").getAsMember();
        if (member == null) {
        	return Responses.error(event, "Cannot report a user who is not a member of this server");
        }
        User author = event.getUser();
        MessageChannel reportChannel = Bot.config.get(event.getGuild()).getModeration().getReportChannel();

        var e = new EmbedBuilder()
            .setAuthor(member.getUser().getAsTag() + " | Report", null, member.getUser().getEffectiveAvatarUrl())
            .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
            .addField("Member", member.getUser().getAsMention(), true)
            .addField("Reported by", author.getAsMention(), true)
            .addField("Channel", event.getTextChannel().getAsMention(), true)
            .addField("Reported on", "<t:" + Instant.now().getEpochSecond() + ":F>", false)
            .addField("ID", "```" + member.getId() + "```", true)
            .addField("Reason", "```" + reason + "```", false)
            .setFooter(author.getAsTag(), author.getEffectiveAvatarUrl())
            .setTimestamp(Instant.now());

        reportChannel.sendMessage("@here").setEmbeds(e.build())
                .setActionRow(
                        Button.danger("utils:ban:" + member.getId(), "Ban"),
                        Button.danger("utils:kick:" + member.getId(), "Kick"),
                        Button.secondary("utils:delete", "🗑️")
                        )
                .queue();

        e.setDescription("Successfully reported " + "``" + member.getUser().getAsTag() + "``!\nYour report has been send to our Moderators");
        return event.replyEmbeds(e.build()).setEphemeral(true);
    }
}


