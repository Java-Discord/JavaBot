package net.javadiscord.javabot.systems.moderation.warn;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * <h3>This class represents the /warns command.</h3>
 * This Command allows users to see all their active warns.
 */
public class WarnsListCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public WarnsListCommand() {
		setSlashCommandData(Commands.slash("warns", "Shows a list of all recent warning.")
				.addOption(OptionType.USER, "user", "If given, shows the recent warns of the given user instead.", false)
				.setGuildOnly(true)
		);
	}

	/**
	 * Builds an {@link MessageEmbed} which contains all recents warnings of a user.
	 *
	 * @param warns A {@link List} with all {@link Warn}s.
	 * @param user  The corresponding {@link User}.
	 * @return The fully-built {@link MessageEmbed}.
	 */
	protected static @NotNull MessageEmbed buildWarnsEmbed(@Nonnull List<Warn> warns, @Nonnull User user) {
		EmbedBuilder builder = new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("Recent Warns")
				.setDescription(String.format("%s has `%s` active warns with a total of `%s` severity.\n",
						user.getAsMention(), warns.size(), warns.stream().mapToInt(Warn::getSeverityWeight).sum()))
				.setColor(Responses.Type.WARN.getColor())
				.setTimestamp(Instant.now());
		warns.forEach(w -> builder.getDescriptionBuilder().append(
				String.format("\n`%s` <t:%s>\nWarned by: <@%s>\nSeverity: `%s (%s)`\nReason: %s\n",
						w.getId(), w.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond(),
						w.getWarnedBy(), w.getSeverity(), w.getSeverityWeight(), w.getReason())));
		return builder.build();
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		User user = event.getOption("user", event::getUser, OptionMapping::getAsUser);
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		event.deferReply(false).queue();
		LocalDateTime cutoff = LocalDateTime.now().minusDays(Bot.getConfig().get(event.getGuild()).getModerationConfig().getWarnTimeoutDays());
		DbHelper.doDaoAction(WarnRepository::new, dao ->
				event.getHook().sendMessageEmbeds(buildWarnsEmbed(dao.getWarnsByUserId(user.getIdLong(), cutoff), user)).queue());
	}
}
