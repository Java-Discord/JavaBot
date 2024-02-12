package net.discordjug.javabot.systems.moderation.warn;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.systems.moderation.warn.dao.WarnRepository;
import net.discordjug.javabot.systems.moderation.warn.model.Warn;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * <h3>This class represents the /warns command.</h3>
 * This Command allows users to see all their active warns.
 */
public class WarnsListCommand extends SlashCommand {
	private final BotConfig botConfig;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;
	private final NotificationService notificationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param warnRepository DAO for interacting with the set of {@link Warn} objects.
	 * @param notificationService service object for notifying users
	 */
	public WarnsListCommand(BotConfig botConfig, ExecutorService asyncPool, WarnRepository warnRepository, NotificationService notificationService) {
		this.botConfig = botConfig;
		this.warnRepository = warnRepository;
		this.asyncPool = asyncPool;
		this.notificationService = notificationService;
		setCommandData(Commands.slash("warns", "Shows a list of all recent warning.")
				.addOption(OptionType.USER, "user", "If given, shows the recent warns of the given user instead.", false)
				.setGuildOnly(true)
		);
	}

	/**
	 * Builds an {@link MessageEmbed} which contains all recents warnings of a user.
	 *
	 * @param severityInformation object containing information about active warns and their severities
	 * @param user  The corresponding {@link User}.
	 * @return The fully-built {@link MessageEmbed}.
	 */
	protected static @NotNull MessageEmbed buildWarnsEmbed(@Nonnull ModerationService.SeverityInformation severityInformation, @Nonnull User user) {
		List<Warn> warns = severityInformation.contributingWarns();
		EmbedBuilder builder = new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(user), null, user.getEffectiveAvatarUrl())
				.setTitle("Recent Warns")
				.setDescription(String.format("%s has `%s` active warns with a total of `%s` severity.\n",
						user.getAsMention(), warns.size(), severityInformation.totalSeverity()))
				.setColor(Responses.Type.WARN.getColor())
				.setTimestamp(Instant.now());
		
		if (severityInformation.severityDiscount() > 0) {
			builder.appendDescription("(Due to warn decay, the total severity is `"+severityInformation.severityDiscount()+"` lower than the sum of all warn severities.)\n");
		}
		
		warns.forEach(w -> {
			String text = String.format("\n`%s` <t:%s>\nWarned by: <@%s>\nSeverity: `%s (%s)`\nReason: %s\n",
					w.getId(), w.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond(),
					w.getWarnedBy(), w.getSeverity(), w.getSeverityWeight(), w.getReason());
			StringBuilder descriptionBuilder = builder.getDescriptionBuilder();
			if (descriptionBuilder.length() + text.length() < MessageEmbed.DESCRIPTION_MAX_LENGTH) {
				descriptionBuilder.append(text);
			} else {
				builder.setFooter("Some warns have been omitted due to length limitations. Contact staff in case you want to see an exhaustive list of warns.");
			}
		});
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
		ModerationService moderationService = new ModerationService(notificationService, botConfig, event, warnRepository, asyncPool);
		asyncPool.execute(() -> {
			try {
				event.getHook().sendMessageEmbeds(buildWarnsEmbed(moderationService.getTotalSeverityWeight(user.getIdLong()), user)).queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, WarnsListCommand.class.getSimpleName());
			}
		});
	}
}
