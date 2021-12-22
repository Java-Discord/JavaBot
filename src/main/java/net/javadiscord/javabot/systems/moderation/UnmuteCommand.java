package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.util.Misc;

import java.time.Instant;

@Deprecated(forRemoval = true)
public class UnmuteCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        Role muteRole = Bot.config.get(event.getGuild()).getModeration().getMuteRole();
        Member member = event.getOption("user").getAsMember();
        try {
            var e = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Unmute", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getErrorColor())
                .addField("Member", member.getAsMention(), true)
                .addField("Moderator", event.getMember().getAsMention(), true)
                .addField("ID", "```" + member.getId() + "```", false)
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

            if (member.getRoles().toString().contains(muteRole.getId())) {
                event.getGuild().removeRoleFromMember(member.getId(), muteRole).queue();

                member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(e).queue());

                Misc.sendToLog(event.getGuild(), e);
                return event.replyEmbeds(e);
            } else return Responses.error(event, "```Can't unmute " + member.getUser().getAsTag() + ", they aren't muted.```");

        } catch (HierarchyException e) {
            return Responses.error(event, e.getMessage());
        }
    }
}
