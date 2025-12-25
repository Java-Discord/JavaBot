package net.discordjug.javabot.systems.user_commands.leaderboard;

import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.help.dao.HelpAccountRepository;
import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Pair;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

/**
 * <h3>This class represents the /leaderboard help-experience command.</h3>
 */
@AutoDetectableComponentHandler("experience-leaderboard")
public class ExperienceLeaderboardSubcommand extends SlashCommand.Subcommand implements ButtonHandler {
	/**
	 * prefix contained in the image cache.
	 */
	public static final String CACHE_PREFIX = "xp_leaderboard";
	private static final int PAGE_SIZE = 10;

	private final BotConfig botConfig;
	private final ExecutorService asyncPool;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig main configuration of the bot.
	 * @param helpAccountRepository Dao object that represents the HELP_ACCOUNT SQL Table.
	 * @param asyncPool the main thread pool for asynchronous operations
	 * @param helpTransactionRepository Dao object that represents the HELP_TRANSACTIONS SQL Table.
	 */
	public ExperienceLeaderboardSubcommand(BotConfig botConfig, HelpAccountRepository helpAccountRepository, ExecutorService asyncPool, HelpTransactionRepository helpTransactionRepository) {
		this.botConfig = botConfig;
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
				Pair<MessageEmbed, FileUpload> messageInfo = buildExperienceLeaderboard(event.getGuild(), page, type);
				event.getHook()
						.editOriginal(new MessageEditBuilder().setEmbeds(messageInfo.first()).setAttachments(messageInfo.second()).build())
						.setComponents(buildPageControls(page, type)).queue();
			} catch (DataAccessException | IOException e) {
				ExceptionLogger.capture(e, ExperienceLeaderboardSubcommand.class.getSimpleName());
			}
		});
	}

	private @NotNull Pair<MessageEmbed, FileUpload> buildExperienceLeaderboard(Guild guild, int page, LeaderboardType type) throws DataAccessException, IOException {
		return switch (type) {
			case TOTAL -> buildGenericExperienceLeaderboard(page, helpAccountRepository.getTotalAccounts(),
					"total Leaderboard of help experience",
					helpAccountRepository::getAccounts, (position, account) -> {
				Pair<Role, Double> currentRole = account.getCurrentExperienceGoal(botConfig, guild);
				return createUserData(guild, position, account.getExperience(), account.getUserId(), currentRole.first() != null ? currentRole.first().getAsMention() + ": " : "");
			});
			case MONTH -> buildGenericExperienceLeaderboard(page, helpTransactionRepository.getNumberOfUsersWithHelpXPInLastMonth(),
					"""
					help experience leaderboard from the last 30 days
					This leaderboard does not include experience decay.
					""",
					helpTransactionRepository::getTotalTransactionWeightsInLastMonth, (position, xpInfo) -> {
				return createUserData(guild, position, (double) xpInfo.second(), xpInfo.first(), "");
			});
		};
	}

	private <T> @NotNull Pair<MessageEmbed, FileUpload> buildGenericExperienceLeaderboard(int page, int totalAccounts, String description,
			BiFunction<Integer, Integer, List<T>> accountsReader, BiFunction<Integer, T, UserData> fieldExtractor) throws DataAccessException, IOException {

		int maxPage = totalAccounts / PAGE_SIZE;
		int actualPage = Math.max(1, Math.min(page, maxPage));
		List<T> accounts = accountsReader.apply(actualPage, PAGE_SIZE);

		EmbedBuilder builder = new EmbedBuilder()
				.setTitle("Experience Leaderboard")
				.setDescription(description)
				.setColor(Responses.Type.DEFAULT.getColor())
				.setFooter(String.format("Page %s/%s", Math.min(page, maxPage), maxPage));

		String pageCachePrefix = CACHE_PREFIX + "_" + page;
		String cacheName = pageCachePrefix + "_" + accounts.hashCode();
		byte[] bytes = LeaderboardCreator.attemptLoadFromCache(cacheName, ()->{
			try (LeaderboardCreator creator = new LeaderboardCreator(accounts.size(), null)){
				for (int i = 0; i < accounts.size(); i++) {
					int position = (i + 1) + (actualPage - 1) * PAGE_SIZE;
					UserData userInfo = fieldExtractor.apply(position, accounts.get(i));
					creator.drawLeaderboardEntry(userInfo.member(), userInfo.displayName(), userInfo.xp(), position);
				}
				return creator.getImageBytes(cacheName, pageCachePrefix);
			}
		});
		builder.setImage("attachment://leaderboard.png");
		return new Pair<MessageEmbed, FileUpload>(builder.build(), FileUpload.fromData(bytes, "leaderboard.png"));
	}

	private UserData createUserData(Guild guild, Integer position, double experience, long userId, String prefix) {
		Member member = guild.retrieveMemberById(userId).onErrorMap(e -> null).complete();
		String displayName;
		if (member == null) {
			displayName = String.valueOf(userId);
		} else {
			displayName = UserUtils.getUserTag(member.getUser());
		}
		return new UserData(member, displayName, (long)experience);
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
				Pair<MessageEmbed, FileUpload> messageInfo = buildExperienceLeaderboard(event.getGuild(), page, type);
				event.getHook().sendMessageEmbeds(messageInfo.first())
					.addFiles(messageInfo.second())
					.setComponents(buildPageControls(page, type))
					.queue();
			}catch (DataAccessException | IOException e) {
				ExceptionLogger.capture(e, ExperienceLeaderboardSubcommand.class.getSimpleName());
			}
		});
	}

	private enum LeaderboardType{
		TOTAL, MONTH
	}

	private record UserData(Member member, String displayName, long xp) {
		UserData {
			Objects.requireNonNull(displayName);
		}
	}
}
