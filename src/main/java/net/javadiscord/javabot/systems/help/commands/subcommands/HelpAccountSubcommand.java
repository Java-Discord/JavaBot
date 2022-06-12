package net.javadiscord.javabot.systems.help.commands.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.StringUtils;

import java.sql.SQLException;

/**
 * Handles commands to show information about how a user has been thanked for
 * their help.
 */
public class HelpAccountSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		User user = event.getOption("user", event::getUser, OptionMapping::getAsUser);
		long totalThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ?",
				s -> s.setLong(1, user.getIdLong())
		);
		long weekThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ? AND thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))",
				s -> s.setLong(1, user.getIdLong())
		);
		try {
			HelpAccount account = new HelpExperienceService(Bot.dataSource).getOrCreateAccount(user.getIdLong());
			return event.replyEmbeds(this.buildHelpAccountEmbed(account, user, event.getGuild(), totalThanks, weekThanks));
		} catch (SQLException e) {
			return Responses.error(event, e.getMessage());
		}
	}

	private MessageEmbed buildHelpAccountEmbed(HelpAccount account, User user, Guild guild, long totalThanks, long weekThanks) {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("Help Account")
				.setThumbnail(user.getEffectiveAvatarUrl())
				.setDescription("Here are some statistics about how you've helped others here.")
				.addField("Experience (BETA)", String.format("%s\n\n**Recent Transactions**\n```diff\n%s```",
						this.formatExperience(guild, account),
						this.formatTransactionHistory(user.getIdLong())), false)
				.addField("Total Times Thanked", String.format("**%s**", totalThanks), true)
				.addField("Times Thanked This Week", String.format("**%s**", weekThanks), true)
				.build();
	}

	private String formatTransactionHistory(long userId) {
		StringBuilder sb = new StringBuilder();
		try {
			HelpExperienceService service = new HelpExperienceService(Bot.dataSource);
			for (HelpTransaction t :service.getRecentTransactions(userId, 3)) {
				sb.append(t.format()).append("\n\n");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sb.toString().length() > 0 ? sb.toString() : "No recent transactions";
	}

	private String formatExperience(Guild guild, HelpAccount account) {
		double currentXp = account.getExperience() - account.getLastExperienceGoal(guild);
		Pair<Role, Double> currentRoleAndXp = account.getCurrentExperienceGoal(guild);
		Pair<Role, Double> nextRoleAndXp = account.getNextExperienceGoal(guild);
		double goalXp = nextRoleAndXp.second() - account.getLastExperienceGoal(guild);
		StringBuilder sb = new StringBuilder();

		// Show the current experience level on the first line.
		sb.append(String.format("%s\n", currentRoleAndXp.first().getAsMention()));

		// Below, show the progress to the next level, or just the XP if they've reached the max level.
		if (goalXp > 0) {
			double percentToGoalXp = (currentXp / goalXp) * 100.0;
			sb.append(String.format("%.0f / %.0f XP (%.2f%%) until %s\n", currentXp, goalXp, percentToGoalXp, nextRoleAndXp.first().getAsMention()))
					.append(String.format("%.0f Total XP\n", account.getExperience()))
					.append(StringUtils.buildTextProgressBar(goalXp / currentXp, 12));
		} else {
			sb.append(String.format("%.0f Total XP (MAX. LEVEL)", account.getExperience()));
		}
		return sb.toString();
	}
}
