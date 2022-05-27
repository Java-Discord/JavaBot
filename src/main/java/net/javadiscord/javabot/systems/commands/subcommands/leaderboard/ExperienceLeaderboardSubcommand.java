package net.javadiscord.javabot.systems.commands.subcommands.leaderboard;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;

import java.sql.SQLException;
import java.util.List;

/**
 * Command that generates a leaderboard based on the help channel experience.
 */
public class ExperienceLeaderboardSubcommand implements SlashCommand {

	private static final int PAGE_SIZE = 5;

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		int page = event.getOption("page", 1, OptionMapping::getAsInt);
		DbHelper.doDaoAction(HelpAccountRepository::new, dao ->
				event.getHook().sendMessageEmbeds(buildExperienceLeaderboard(event.getGuild(), dao, page))
						.addActionRows(buildPageControls(page))
						.queue());
		return event.deferReply(false);
	}

	/**
	 * Handles all Button Interactions that regard this command.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired upon use.
	 * @param id    The component's id, split by ":".
	 */
	public static void handleButtons(ButtonInteractionEvent event, String[] id) {
		event.deferEdit().queue();
		DbHelper.doDaoAction(HelpAccountRepository::new, dao -> {
			int page = Integer.parseInt(id[2]);
			switch (id[1]) {
				case "left" -> page--;
				case "right" -> page++;
			}
			int maxPage = dao.getTotalAccounts() / PAGE_SIZE;
			if (page <= 0) page = maxPage;
			if (page > maxPage) page = 1;
			event.getHook().editOriginalEmbeds(buildExperienceLeaderboard(event.getGuild(), dao, page))
					.setActionRows(buildPageControls(page))
					.queue();
		});
	}

	private static MessageEmbed buildExperienceLeaderboard(Guild guild, HelpAccountRepository dao, int page) throws SQLException {
		int maxPage = dao.getTotalAccounts() / PAGE_SIZE;
		List<HelpAccount> accounts = dao.getAccounts(Math.min(page, maxPage), PAGE_SIZE);
		EmbedBuilder builder = new EmbedBuilder()
				.setTitle("Experience Leaderboard")
				.setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
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

	private static ActionRow buildPageControls(int currentPage) {
		return ActionRow.of(
				new ButtonImpl("experience-leaderboard:left:" + currentPage, "", ButtonStyle.SECONDARY, false, Emoji.fromUnicode("⬅")),
				new ButtonImpl("experience-leaderboard:right:" + currentPage, "", ButtonStyle.SECONDARY, false, Emoji.fromUnicode("➡"))
		);
	}
}
