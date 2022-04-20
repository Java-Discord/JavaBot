package net.javadiscord.javabot.systems.help.commands.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
			int maxPage = dao.getTotalRankedAccounts() / PAGE_SIZE;
			if (page <= 0) page = maxPage;
			if (page > maxPage) page = 1;
			event.getHook().editOriginalEmbeds(buildExperienceLeaderboard(event.getGuild(), dao, page))
					.setActionRows(buildPageControls(page))
					.queue();
		});
	}

	private static MessageEmbed buildExperienceLeaderboard(Guild guild, HelpAccountRepository dao, int page) throws SQLException {
		int maxPage = dao.getTotalRankedAccounts() / PAGE_SIZE;
		List<HelpAccount> accounts = dao.getAccountsWithRank(Math.min(page, maxPage), PAGE_SIZE);
		EmbedBuilder builder = new EmbedBuilder()
				.setTitle("Experience Leaderboard")
				.setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
				.setFooter(String.format("Page %s/%s", Math.min(page, maxPage), maxPage));
		accounts.forEach(account -> {
			Map.Entry<Long, Double> currentRole = account.getCurrentExperienceGoal(guild);
			builder.addField(
					// TODO: implement without using blocking complete calls
					String.format("**%s.** %s", (accounts.indexOf(account) + 1) + (page - 1) * PAGE_SIZE, guild.getJDA().retrieveUserById(account.getUserId()).complete().getAsTag()),
					String.format("<@&%s>: `%.0f XP`\n", currentRole.getKey(), account.getExperience()),
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
