package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * <h3>This class represents the /help account command.</h3>
 * Handles commands to show information about how a user has been thanked for
 * their help.
 */
public class HelpAccountSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public HelpAccountSubcommand() {
		setSubcommandData(new SubcommandData("account", "Shows an overview of your Help Account.")
				.addOption(OptionType.USER, "user", "If set, show the Help Account of the specified user instead.", false)
				.addOption(OptionType.BOOLEAN, "show-transactions", "Should the recent transactions be shown?", false)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		User user = event.getOption("user", event::getUser, OptionMapping::getAsUser);
		boolean showTransactions = event.getOption("show-transactions", false, OptionMapping::getAsBoolean);
		long totalThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ?",
				s -> s.setLong(1, user.getIdLong())
		);
		long weekThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ? AND thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))",
				s -> s.setLong(1, user.getIdLong())
		);
		try {
			HelpAccount account = new HelpExperienceService(Bot.getDataSource()).getOrCreateAccount(user.getIdLong());
			event.replyEmbeds(buildHelpAccountEmbed(account, user, event.getGuild(), totalThanks, weekThanks, showTransactions)).queue();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, e.getMessage()).queue();
		}
	}

	private @NotNull MessageEmbed buildHelpAccountEmbed(HelpAccount account, @NotNull User user, Guild guild, long totalThanks, long weekThanks, boolean showTransactions) {
		return  new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("Help Account")
				.setThumbnail(user.getEffectiveAvatarUrl())
				.setDescription("Here are some statistics about how you've helped others here.")
				.addField("Experience (BETA)", String.format("%s%s",
					formatExperience(guild, account),
					showTransactions ? String.format("\n\n**Recent Transactions**\n```diff\n%s```", formatTransactionHistory(user.getIdLong())) : ""), false)
				.addField("Total Times Thanked", String.format("**%s**", totalThanks), true)
				.addField("Times Thanked This Week", String.format("**%s**", weekThanks), true)
				.build();
	}

	private @NotNull String formatTransactionHistory(long userId) {
		StringBuilder sb = new StringBuilder();
		try {
			HelpExperienceService service = new HelpExperienceService(Bot.getDataSource());
			for (HelpTransaction t :service.getRecentTransactions(userId, 3)) {
				sb.append(t.format()).append("\n\n");
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
		return sb.toString().length() > 0 ? sb.toString() : "No recent transactions";
	}

	private String formatExperience(Guild guild, HelpAccount account) {
		double currentXp = account.getExperience() - account.getPreviousExperienceGoal(guild).second();
		Pair<Role, Double> currentRoleAndXp = account.getCurrentExperienceGoal(guild);
		Pair<Role, Double> nextRoleAndXp = account.getNextExperienceGoal(guild);
		double goalXp = nextRoleAndXp.second() - account.getPreviousExperienceGoal(guild).second();
		StringBuilder sb = new StringBuilder();

		if (currentRoleAndXp.first() != null) {
			// Show the current experience level on the first line.
			sb.append(String.format("%s\n", currentRoleAndXp.first().getAsMention()));
		}
		// Below, show the progress to the next level, or just the XP if they've reached the max level.
		if (goalXp > 0) {
			double percentToGoalXp = (currentXp / goalXp) * 100.0;
			sb.append(String.format("%.0f / %.0f XP (%.2f%%) until %s\n", currentXp, goalXp, percentToGoalXp, nextRoleAndXp.first().getAsMention()))
					.append(String.format("%.0f Total XP\n", account.getExperience()))
					.append(StringUtils.buildTextProgressBar(percentToGoalXp / 100.0, 20));
		} else {
			sb.append(String.format("%.0f Total XP (MAX. LEVEL)", account.getExperience()));
		}
		return sb.toString();
	}
}
