package net.javadiscord.javabot.systems.moderation.warn;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.command.interfaces.UserContextCommand;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Command that allows users to see all their active warns.
 */
public class WarnsCommand implements SlashCommand, UserContextCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		Member member = event.getOption("user", event::getMember, OptionMapping::getAsMember);
		if (member == null) return Responses.error(event, "Member is missing.");
		LocalDateTime cutoff = LocalDateTime.now().minusDays(Bot.config.get(event.getGuild()).getModeration().getWarnTimeoutDays());
		try (var con = Bot.dataSource.getConnection()) {
			return event.replyEmbeds(buildWarnsEmbed(new WarnRepository(con)
					.getWarnsByUserId(member.getIdLong(), cutoff), member));
		} catch (SQLException e) {
			throw ResponseException.error("Could not get warns from user: " + member.getUser().getAsTag(), e);
		}
	}

	@Override
	public InteractionCallbackAction<?> handleUserContextCommandInteraction(UserContextInteractionEvent event) throws ResponseException {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(Bot.config.get(event.getGuild()).getModeration().getWarnTimeoutDays());
		Member member = event.getTargetMember();
		try (var con = Bot.dataSource.getConnection()) {
			return event.replyEmbeds(buildWarnsEmbed(new WarnRepository(con)
					.getWarnsByUserId(member.getIdLong(), cutoff), member));
		} catch (SQLException e) {
			throw ResponseException.error("Could not get warns from user: " + member.getUser().getAsTag(), e);
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
