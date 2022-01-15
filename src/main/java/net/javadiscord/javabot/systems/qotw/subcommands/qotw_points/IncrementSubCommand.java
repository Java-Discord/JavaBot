package net.javadiscord.javabot.systems.qotw.subcommands.qotw_points;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.commands.LeaderboardCommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.util.Misc;

import java.sql.SQLException;
import java.time.Instant;

public class IncrementSubCommand implements SlashCommandHandler {

    public long correct(Member member, boolean quiet) {
        try (var con = Bot.dataSource.getConnection()) {
            var repo = new QuestionPointsRepository(con);
            var memberId = member.getIdLong();
            repo.increment(memberId);
            var points = repo.getAccountByUserId(memberId).getPoints();
            var dmEmbed = buildIncrementDmEmbed(member, points);
            var embed = buildIncrementEmbed(member, points);
            if (!quiet) Misc.sendToLog(member.getGuild(), embed);
            member.getUser().openPrivateChannel().queue(
                    c -> c.sendMessageEmbeds(dmEmbed).queue(),
                    e -> Misc.sendToLog(member.getGuild(), "> Could not send direct message to member " + member.getAsMention()));
            return repo.getAccountByUserId(memberId).getPoints();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var memberOption = event.getOption("user");
        if (memberOption == null) {
            return Responses.error(event, "Missing required arguments.");
        }
        var member = memberOption.getAsMember();
        var points = correct(member, false);
        var embed = buildIncrementEmbed(member, points);
        return event.replyEmbeds(embed);
    }

    private MessageEmbed buildIncrementDmEmbed(Member member, long points) {
        return new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag(), null, member.getUser().getEffectiveAvatarUrl())
                .setTitle("Question of the Week")
                .setColor(Bot.config.get(member.getGuild()).getSlashCommand().getSuccessColor())
                .setDescription(String.format(
                        "Your answer was correct! %s\nYou've been granted **`1 QOTW-Point`**! (total: %s)",
                                Bot.config.get(member.getGuild()).getEmote().getSuccessEmote().getAsMention(), points))
                .setTimestamp(Instant.now())
                .build();
    }

    private MessageEmbed buildIncrementEmbed(Member member, long points) {
        return new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | QOTW-Point added", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Bot.config.get(member.getGuild()).getSlashCommand().getSuccessColor())
                .addField("Total QOTW-Points", "```" + points + "```", true)
                .addField("Rank", "```#" + new LeaderboardCommand().getQOTWRank(member.getIdLong()) + "```", true)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();
    }

}
