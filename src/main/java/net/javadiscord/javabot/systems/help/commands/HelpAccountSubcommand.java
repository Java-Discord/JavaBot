package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Plotter;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * <h3>This class represents the /help account command.</h3>
 * Handles commands to show information about how a user has been thanked for
 * their help.
 */
public class HelpAccountSubcommand extends SlashCommand.Subcommand {

	private final BotConfig botConfig;
	private final DbActions dbActions;
	private final HelpExperienceService helpExperienceService;
	private final HelpTransactionRepository transactionRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 *
	 * @param botConfig             The bot configuration
	 * @param dbActions             An object responsible for various database actions
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 * @param transactionRepository DAO for help XP transactions
	 */
	public HelpAccountSubcommand(BotConfig botConfig, DbActions dbActions, HelpExperienceService helpExperienceService, HelpTransactionRepository transactionRepository) {
		this.dbActions = dbActions;
		this.helpExperienceService = helpExperienceService;
		this.botConfig = botConfig;
		this.transactionRepository = transactionRepository;
		setCommandData(new SubcommandData("account", "Shows an overview of your Help Account.")
				.addOption(OptionType.USER, "user", "If set, show the Help Account of the specified user instead.", false)
				.addOption(OptionType.BOOLEAN, "plot", "generate a plot of help XP history", false)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		User user = event.getOption("user", event::getUser, OptionMapping::getAsUser);
		boolean plot = event.getOption("plot", false, OptionMapping::getAsBoolean);
		
		if (plot && user.getIdLong()!=event.getUser().getIdLong() && !Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.error(event, "You can only plot your own help XP history.").queue();
			return;
		}
		
		long totalThanks = dbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ?",
				s -> s.setLong(1, user.getIdLong())
		);
		long weekThanks = dbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ? AND thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))",
				s -> s.setLong(1, user.getIdLong())
		);
		
		event.deferReply().queue();
		
		FileUpload upload = null;
		if (plot) {
			upload = generatePlot(user);
		}
		
		try {
			HelpAccount account = helpExperienceService.getOrCreateAccount(user.getIdLong());
			WebhookMessageCreateAction<Message> reply = event.getHook().sendMessageEmbeds(buildHelpAccountEmbed(account, user, event.getGuild(), totalThanks, weekThanks));
			if (upload!=null) {
				reply.addFiles(upload);
			}
			reply.queue();
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, e.getMessage()).queue();
		}
	}

	private FileUpload generatePlot(User user) {
		List<Pair<Pair<Integer,Integer>,Double>> xpData = transactionRepository.getTotalTransactionWeightByMonth(user.getIdLong(), LocalDate.now().withDayOfMonth(1).minusYears(1).atStartOfDay());
		
		if (xpData.isEmpty()) {
			return null;
		}
		
		List<Pair<String, Double>> plotData = new ArrayList<>();
		
		int i = 0;
		for(LocalDate position = LocalDate.now().minusYears(1); position.isBefore(LocalDate.now().plusDays(1)); position=position.plusMonths(1)) {
			double value = 0.0;
			if(i<xpData.size()) {
				Pair<Pair<Integer, Integer>, Double> entry = xpData.get(i);
				if(entry.first().first() == position.getMonthValue() && entry.first().second() == position.getYear()) {
					value = Math.round(entry.second()*100)/100.0;
					i++;
				}
			}
			plotData.add(new Pair<>(position.getMonth() + " " + position.getYear(), value));
		}
		
		BufferedImage plt = new Plotter(plotData, "gained help XP per month").plot();
		try(ByteArrayOutputStream os = new ByteArrayOutputStream()){
			ImageIO.write(plt, "png", os);
			return FileUpload.fromData(os.toByteArray(), "image.png");
		} catch (IOException e) {
			ExceptionLogger.capture(e, "Cannot create XP plot");
		}
		return null;
	}

	private @NotNull MessageEmbed buildHelpAccountEmbed(HelpAccount account, @NotNull User user, Guild guild, long totalThanks, long weekThanks) {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
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
