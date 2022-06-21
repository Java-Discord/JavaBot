package net.javadiscord.javabot.systems.user_commands.leaderboard;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
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
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

/**
 * <h3>This class represents the /leaderboard help-experience command.</h3>
 */
public class ExperienceLeaderboardSubcommand extends SlashCommand.Subcommand {
	private static final int PAGE_SIZE = 5;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public ExperienceLeaderboardSubcommand() {
		setSubcommandData(new SubcommandData("help-experience", "The Help Experience Leaderboard.")
				.addOption(OptionType.INTEGER, "page", "The page of results to show. By default it starts at 1.", false)
		);
	}

	/**
	 * Handles all Button Interactions that regard this command.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired upon use.
	 * @param id    The component's id, split by ":".
	 */
	public static void handleButtons(@NotNull ButtonInteractionEvent event, String[] id) {
		event.deferEdit().queue();
		DbHelper.doDaoAction(HelpAccountRepository::new, dao -> {
			int page = Integer.parseInt(id[2]);
			// increment/decrement page
			if (id[1].equals("left")) {
				page--;
			} else {
				page++;
			}
			int maxPage = dao.getTotalAccounts() / PAGE_SIZE;
			if (page <= 0) page = maxPage;
			if (page > maxPage) page = 1;
			event.getHook().editOriginalEmbeds(buildExperienceLeaderboard(event.getGuild(), dao, page))
					.setActionRows(buildPageControls(page))
					.queue();
		});
	}

	private static @NotNull MessageEmbed buildExperienceLeaderboard(Guild guild, @NotNull HelpAccountRepository dao, int page) throws SQLException {
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
		DbHelper.doDaoAction(HelpAccountRepository::new, dao ->
				event.getHook().sendMessageEmbeds(buildExperienceLeaderboard(event.getGuild(), dao, page))
						.addActionRows(buildPageControls(page))
						.queue());
	}
}
