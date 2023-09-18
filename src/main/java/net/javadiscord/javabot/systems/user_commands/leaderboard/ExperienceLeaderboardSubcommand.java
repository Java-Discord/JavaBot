package net.javadiscord.javabot.systems.user_commands.leaderboard;

import net.javadiscord.javabot.util.UserUtils;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

/**
 * <h3>This class represents the /leaderboard help-experience command.</h3>
 */
@AutoDetectableComponentHandler("experience-leaderboard")
public class ExperienceLeaderboardSubcommand extends SlashCommand.Subcommand implements ButtonHandler {
	private static final int PAGE_SIZE = 5;
	private final ExecutorService asyncPool;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param helpAccountRepository Dao object that represents the HELP_ACCOUNT SQL Table.
	 * @param asyncPool the main thread pool for asynchronous operations
	 * @param helpTransactionRepository Dao object that represents the HELP_TRANSACTIONS SQL Table.
	 */
	public ExperienceLeaderboardSubcommand(HelpAccountRepository helpAccountRepository, ExecutorService asyncPool, HelpTransactionRepository helpTransactionRepository) {
		this.asyncPool = asyncPool;
		this.helpAccountRepository = helpAccountRepository;
		this.helpTransactionRepository = helpTransactionRepository;
		setCommandData(new SubcommandData("help-experience", "The Help Experience Leaderboard.")
				.addOption(OptionType.INTEGER, "page", "The page of results to show. By default it starts at 1.", false)
				.addOptions(new OptionData(OptionType.STRING, "type", "Type of the help-XP headerboard", false)
						.addChoice("total", LeaderboardType.TOTAL.name())
						.addChoice("last 30 days", LeaderboardType.MONTH.name()))
		);
	}

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, Button button) {
		event.deferEdit().queue();
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		LeaderboardType type;
		if (id.length > 3) {
			type = LeaderboardType.valueOf(id[3]);
		} else {
			type = LeaderboardType.TOTAL;
		}
		asyncPool.execute(() -> {
			try {
				int page = Integer.parseInt(id[2]);
				// increment/decrement page
				if (id[1].equals("left")) {
					page--;
				} else {
					page++;
				}
				int totalAccounts = switch (type) {
					case MONTH -> helpTransactionRepository.getNumberOfUsersWithHelpXPInLastMonth();
					case TOTAL -> helpAccountRepository.getTotalAccounts();
				};
				int maxPage = totalAccounts / PAGE_SIZE;
				if (page <= 0) {
					page = maxPage;
				}
				if (page > maxPage) {
					page = 1;
				}
				event.getHook()
						.editOriginalEmbeds(buildExperienceLeaderboard(event.getGuild(), page, type))
						.setComponents(buildPageControls(page, type)).queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, ExperienceLeaderboardSubcommand.class.getSimpleName());
			}
		});
	}


	private @NotNull MessageEmbed buildExperienceLeaderboard(Guild guild, int page, LeaderboardType type) throws DataAccessException {
		return switch (type) {
			case TOTAL -> buildGenericExperienceLeaderboard(page, helpAccountRepository.getTotalAccounts(),
					"total Leaderboard of help experience",
					helpAccountRepository::getAccounts, (position, account) -> {
				Pair<Role, Double> currentRole = account.getCurrentExperienceGoal(guild);
				return buildEmbed(guild, position, account.getExperience(), account.getUserId(), currentRole.first() != null ? currentRole.first().getAsMention() + ": " : "");
			});
			case MONTH -> buildGenericExperienceLeaderboard(page, helpTransactionRepository.getNumberOfUsersWithHelpXPInLastMonth(),
					"""
					help experience leaderboard from the last 30 days
					This leaderboard does not include experience decay.
					""",
					helpTransactionRepository::getTotalTransactionWeightsInLastMonth, (position, xpInfo) -> {
				return buildEmbed(guild, position, (double) xpInfo.second(), xpInfo.first(), "");
			});
		};
	}

	private <T> @NotNull MessageEmbed buildGenericExperienceLeaderboard(int page, int totalAccounts, String description,
			BiFunction<Integer, Integer, List<T>> accountsReader, BiFunction<Integer, T, MessageEmbed.Field> fieldExtractor) throws DataAccessException {
		int maxPage = totalAccounts / PAGE_SIZE;
		int actualPage = Math.max(1, Math.min(page, maxPage));
		List<T> accounts = accountsReader.apply(actualPage, PAGE_SIZE);
		EmbedBuilder builder = new EmbedBuilder()
				.setTitle("Experience Leaderboard")
				.setDescription(description)
				.setColor(Responses.Type.DEFAULT.getColor())
				.setFooter(String.format("Page %s/%s", actualPage, maxPage));
		for (int i = 0; i < accounts.size(); i++) {
			int position = (i + 1) + (actualPage - 1) * PAGE_SIZE;
			builder.addField(fieldExtractor.apply(position, accounts.get(i)));
		}
		return builder.build();
	}

	private Field buildEmbed(Guild guild, Integer position, double experience, long userId, String prefix) {
		User user = guild.getJDA().getUserById(userId);
		return new MessageEmbed.Field(
				String.format("**%s.** %s", position, user == null ? userId : UserUtils.getUserTag(user)),
				String.format("%s`%.0f XP`\n", prefix, experience),
				false);
	}

	@Contract("_ -> new")
	private static @NotNull ActionRow buildPageControls(int currentPage, LeaderboardType type) {
		return ActionRow.of(
				Button.primary(ComponentIdBuilder.build("experience-leaderboard", "left", currentPage, type.name()), "Prev"),
				Button.primary(ComponentIdBuilder.build("experience-leaderboard", "right", currentPage, type.name()), "Next")
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		int page = event.getOption("page", 1, OptionMapping::getAsInt);
		LeaderboardType type = event.getOption("type", LeaderboardType.TOTAL, o->LeaderboardType.valueOf(o.getAsString()));
		event.deferReply().queue();
		asyncPool.execute(() -> {
			try {
				event.getHook().sendMessageEmbeds(buildExperienceLeaderboard(event.getGuild(), page, type))
					.setComponents(buildPageControls(page, type))
					.queue();
			}catch (DataAccessException e) {
				ExceptionLogger.capture(e, ExperienceLeaderboardSubcommand.class.getSimpleName());
			}
		});
	}

	private enum LeaderboardType{
		TOTAL, MONTH
	}
}
