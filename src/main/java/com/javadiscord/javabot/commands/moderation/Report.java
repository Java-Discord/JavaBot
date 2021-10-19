package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.utils.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;
import java.time.LocalDateTime;

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
            .addField("Name", "```" + member.getUser().getAsTag() + "```", false)
            .addField("ID", "```" + member.getId() + "```", true)
            .addField("Reported by", "```" + author.getAsTag() + "```", true)
            .addField("Channel", "```#" + event.getTextChannel().getName() + "```", true)
            .addField("Reported on", "```" + LocalDateTime.now().format(TimeUtils.STANDARD_FORMATTER) + "```", true)
            .addField("Reason", "```" + reason + "```", false)
            .setFooter(author.getAsTag(), author.getEffectiveAvatarUrl())
            .setTimestamp(Instant.now());

        reportChannel.sendMessage("@here").setEmbeds(e.build()).queue();

        e.setDescription("Successfully reported " + "``" + member.getUser().getAsTag() + "``!\nYour report has been send to our Moderators");
        return event.replyEmbeds(e.build()).setEphemeral(true);
    }
}


