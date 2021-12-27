package net.javadiscord.javabot.systems.moderation.warn;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class WarnsCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		OptionMapping warnsOption = event.getOption("user");
		Member member = warnsOption == null ? event.getMember() : warnsOption.getAsMember();
		if (member == null) return Responses.error(event, "Member is missing.");
		LocalDateTime cutoff = LocalDateTime.now().minusDays(Bot.config.get(event.getGuild()).getModeration().getWarnTimeoutDays());
		try (var con = Bot.dataSource.getConnection()) {
			return event.replyEmbeds(buildWarnsEmbed(new WarnRepository(con)
					.getWarnsByUserId(member.getIdLong(), cutoff), member));
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An Error occurred.");
		}
	}

	private MessageEmbed buildWarnsEmbed(List<Warn> warns, Member member) {
		var e = new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag() + " | Warns", null, member.getUser().getEffectiveAvatarUrl())
				.setDescription(String.format("%s has `%s` active warns with a total of `%s` severity.\n",
						member.getAsMention(), warns.size(), warns.stream().mapToInt(Warn::getSeverityWeight).sum()))
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getWarningColor())
				.setTimestamp(Instant.now());
		warns.forEach(w -> e.getDescriptionBuilder().append(
				String.format("\n`%s` <t:%s>\nWarned by: <@%s>\nSeverity: `%s (%s)`\nReason: %s\n",
						w.getId(), w.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond(),
						w.getWarnedBy(), w.getSeverity(), w.getSeverityWeight(), w.getReason())));
		return e.build();
	}
}
