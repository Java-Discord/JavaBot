package net.javadiscord.javabot.systems.user_commands.leaderboard;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * <h3>This class represents the /leaderboard help-experience command.</h3>
 */
@AutoDetectableComponentHandler("experience-leaderboard")
public class ExperienceLeaderboardSubcommand extends SlashCommand.Subcommand implements ButtonHandler {
	private static final int PAGE_SIZE = 5;
	private final ExecutorService asyncPool;
	private final HelpAccountRepository helpAccountRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param helpAccountRepository Dao object that represents the HELP_ACCOUNT SQL Table.
	 * @param asyncPool the main thread pool for asynchronous operations
	 */
	public ExperienceLeaderboardSubcommand(HelpAccountRepository helpAccountRepository, ExecutorService asyncPool) {
		this.asyncPool = asyncPool;
		this.helpAccountRepository = helpAccountRepository;
		setSubcommandData(new SubcommandData("help-experience", "The Help Experience Leaderboard.")
				.addOption(OptionType.INTEGER, "page", "The page of results to show. By default it starts at 1.", false)
		);
	}

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, Button button) {
		event.deferEdit().queue();
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		asyncPool.execute(() -> {
			try {
				int page = Integer.parseInt(id[2]);
				// increment/decrement page
				if (id[1].equals("left")) {
					page--;
				} else {
					page++;
				}
				int maxPage = helpAccountRepository.getTotalAccounts() / PAGE_SIZE;
				if (page <= 0) page = maxPage;
				if (page > maxPage) page = 1;
				event.getHook().editOriginalEmbeds(buildExperienceLeaderboard(event.getGuild(), helpAccountRepository, page))
						.setActionRows(buildPageControls(page))
						.queue();
			}catch (DataAccessException e) {
				ExceptionLogger.capture(e, ExperienceLeaderboardSubcommand.class.getSimpleName());
			}
		});
	}

	private static @NotNull MessageEmbed buildExperienceLeaderboard(Guild guild, @NotNull HelpAccountRepository dao, int page) throws DataAccessException {
		int maxPage = dao.getTotalAccounts() / PAGE_SIZE;
		List<HelpAccount> accounts = dao.getAccounts(Math.min(page, maxPage), PAGE_SIZE);
		EmbedBuilder builder = new EmbedBuilder()
				.setTitle("Experience Leaderboard")
				.setColor(Responses.Type.DEFAULT.getColor())
				.setFooter(String.format("Page %s/%s", Math.min(page, maxPage), maxPage));
		accounts.forEach(account -> {
			Pair<Role, Double> currentRole = account.getCurrentExperienceGoal(guild);
			User user = guild.getJDA().getUserById(account.getUserId());
			builder.addField(
					String.format("**%s.** %s", (accounts.indexOf(account) + 1) + (page - 1) * PAGE_SIZE, user == null ? account.getUserId() : user.getAsTag()),
					String.format("%s`%.0f XP`\n", currentRole.first() != null ? currentRole.first().getAsMention() + ": " : "", account.getExperience()),
					false);
		});
		return builder.build();
	}

	@Contract("_ -> new")
	private static @NotNull ActionRow buildPageControls(int currentPage) {
		return ActionRow.of(
				Button.primary(ComponentIdBuilder.build("experience-leaderboard", "left", currentPage), "Prev"),
				Button.primary(ComponentIdBuilder.build("experience-leaderboard", "right", currentPage), "Next")
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		int page = event.getOption("page", 1, OptionMapping::getAsInt);
		event.deferReply().queue();
		asyncPool.execute(() -> {
			try {
				event.getHook().sendMessageEmbeds(buildExperienceLeaderboard(event.getGuild(), helpAccountRepository, page))
						.addActionRows(buildPageControls(page))
						.queue();
			}catch (DataAccessException e) {
				ExceptionLogger.capture(e, ExperienceLeaderboardSubcommand.class.getSimpleName());
			}
		});
	}
}
