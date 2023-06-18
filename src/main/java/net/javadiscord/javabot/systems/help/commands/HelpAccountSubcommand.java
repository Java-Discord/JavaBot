package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringUtils;
import net.javadiscord.javabot.util.UserUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * <h3>This class represents the /help account command.</h3>
 * Handles commands to show information about how a user has been thanked for
 * their help.
 */
public class HelpAccountSubcommand extends SlashCommand.Subcommand {

	private final DbActions dbActions;
	private final HelpExperienceService helpExperienceService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 *
	 * @param dbActions             An object responsible for various database actions
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 */
	public HelpAccountSubcommand(DbActions dbActions, HelpExperienceService helpExperienceService) {
		this.dbActions = dbActions;
		this.helpExperienceService = helpExperienceService;
		setCommandData(new SubcommandData("account", "Shows an overview of your Help Account.")
				.addOption(OptionType.USER, "user", "If set, show the Help Account of the specified user instead.", false)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		User user = event.getOption("user", event::getUser, OptionMapping::getAsUser);
		long totalThanks = dbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ?",
				s -> s.setLong(1, user.getIdLong())
		);
		long weekThanks = dbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ? AND thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))",
				s -> s.setLong(1, user.getIdLong())
		);
		try {
			HelpAccount account = helpExperienceService.getOrCreateAccount(user.getIdLong());
			event.replyEmbeds(buildHelpAccountEmbed(account, user, event.getGuild(), totalThanks, weekThanks)).queue();
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, e.getMessage()).queue();
		}
	}

	private @NotNull MessageEmbed buildHelpAccountEmbed(HelpAccount account, @NotNull User user, Guild guild, long totalThanks, long weekThanks) {
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(user), null, user.getEffectiveAvatarUrl())
				.setTitle("Help Account")
				.setThumbnail(user.getEffectiveAvatarUrl())
				.setDescription("Here are some statistics about how you've helped others here.")
				.addField("Experience (BETA)", formatExperience(guild, account), false)
				.addField("Total Times Thanked", String.format("**%s**", totalThanks), true)
				.addField("Times Thanked This Week", String.format("**%s**", weekThanks), true)
				.build();
	}

	private @NotNull String formatExperience(Guild guild, @NotNull HelpAccount account) {
		Pair<Role, Double> previousRoleAndXp = account.getPreviousExperienceGoal(guild);
		Pair<Role, Double> currentRoleAndXp = account.getCurrentExperienceGoal(guild);
		Pair<Role, Double> nextRoleAndXp = account.getNextExperienceGoal(guild);
		double currentXp = account.getExperience() - (previousRoleAndXp == null ? 0 : previousRoleAndXp.second());
		double goalXp = nextRoleAndXp.second() - (previousRoleAndXp == null ? 0 : previousRoleAndXp.second());
		StringBuilder sb = new StringBuilder();

		if (currentRoleAndXp.first() != null) {
			// Show the current experience level on the first line.
			sb.append(String.format("%s%n", currentRoleAndXp.first().getAsMention()));
		}
		// Below, show the progress to the next level, or just the XP if they've reached the max level.
		if (goalXp > 0) {
			double percentToGoalXp = (currentXp / goalXp) * 100.0;
			sb.append(String.format("%.0f / %.0f XP (%.2f%%) until %s%n", currentXp, goalXp, percentToGoalXp, nextRoleAndXp.first().getAsMention()))
					.append(String.format("%.0f Total XP%n", account.getExperience()))
					.append(StringUtils.buildTextProgressBar(percentToGoalXp / 100.0, 20));
		} else {
			sb.append(String.format("%.0f Total XP (MAX. LEVEL)", account.getExperience()));
		}
		return sb.toString();
	}
}
