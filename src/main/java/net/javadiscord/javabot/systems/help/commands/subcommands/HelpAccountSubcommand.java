package net.javadiscord.javabot.systems.help.commands.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
import net.javadiscord.javabot.util.StringUtils;

import java.sql.SQLException;
import java.util.Map;

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
		double current = account.getExperience() - account.getLastExperienceGoal(guild);
		Map.Entry<Long, Double> role = account.getNextExperienceGoal(guild);
		double goal = role.getValue() - account.getLastExperienceGoal(guild);
		StringBuilder sb = new StringBuilder(String.format("<@&%s>: ", role.getKey()));
		if (goal > 0) {
			sb.append(String.format("%.2f XP / %.2f XP (%.2f%%)", current, goal, (current / goal) * 100))
					.append("\n")
					.append(String.format("`[%s]`", StringUtils.buildProgressBar(current, goal, "\u2581", "\u2588", 40)));
		} else {
			sb.append(String.format("%.2f XP (MAX. LEVEL)", account.getExperience()));
		}
		return sb.toString();
	}
}
