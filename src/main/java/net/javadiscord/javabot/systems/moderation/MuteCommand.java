package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.util.Misc;

import java.time.Instant;

@Deprecated(forRemoval = true)
public class MuteCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member member = event.getOption("user").getAsMember();

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        var eb = muteEmbed(member, event.getMember(), event.getGuild(), reason);
        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(eb).queue());

        Role muteRole = Bot.config.get(event.getGuild()).getModeration().getMuteRole();
        if (member.getRoles().contains(muteRole)) {
            return Responses.error(event, "```" + member.getUser().getAsTag() + " is already muted```");
        }

        try {
            new ModerationService(event.getInteraction()).mute(member, event.getGuild());
            return event.replyEmbeds(eb);
        } catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }
    }


    public MessageEmbed muteEmbed(Member member, Member mod, Guild guild, String reason) {
        return new EmbedBuilder()
                .setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
                .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Member", member.getAsMention(), true)
                .addField("Moderator", mod.getAsMention(), true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter(mod.getUser().getAsTag(), mod.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
    }
}