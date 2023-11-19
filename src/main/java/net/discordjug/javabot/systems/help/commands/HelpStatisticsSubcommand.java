package net.discordjug.javabot.systems.help.commands;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository;
import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository.MonthInYear;
import net.discordjug.javabot.systems.help.model.HelpAccount;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Pair;
import net.discordjug.javabot.util.Plotter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * Shows the distribution of help XP per user.
 */
public class HelpStatisticsSubcommand extends SlashCommand.Subcommand {

	private static final List<Pair<String, Color>> COLORS = List.of(
			new Pair<>("Red", Color.RED), new Pair<>("Blue", Color.BLUE), new Pair<>("Yellow", Color.YELLOW),
			new Pair<>("Green", Color.GREEN), new Pair<>("Cyan", Color.CYAN), new Pair<>("Magenta", Color.MAGENTA),
			new Pair<>("Orange", Color.ORANGE), new Pair<>("Pink", Color.PINK), new Pair<>("Light gray", Color.LIGHT_GRAY)
			);

	private final HelpTransactionRepository transactionRepository;
	
	public HelpStatisticsSubcommand(HelpTransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
		setCommandData(new SubcommandData("stats", "Shows an general plot about help activity in this server"));
	}
	
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		
		event.deferReply().queue();
		
		List<Pair<MonthInYear,HelpAccount>> transactionWeights = transactionRepository.getTotalTransactionWeightByMonthAndUsers(LocalDate.now().withDayOfMonth(1).minusYears(1).atStartOfDay());
		
		Map<Long, Pair<String, Color>> topUsersToColors = mapTopUsersToColors(transactionWeights);
		
		List<Pair<String, Plotter.Bar>> plotData = new ArrayList<>();
		
		int i = 0;
		
		for(LocalDate position = LocalDate.now().minusYears(1); position.isBefore(LocalDate.now().plusDays(1)); position=position.plusMonths(1)) {
			List<Pair<Color,Double>> entriesForThisMonth = new ArrayList<>();
			boolean correctMonth = true;
			while(i<transactionWeights.size() && correctMonth) {
				Pair<MonthInYear,HelpAccount> entry = transactionWeights.get(i);
				if(entry.first().month() == position.getMonthValue() && entry.first().year() == position.getYear()) {
					Long userId = entry.second().getUserId();
					Color color = topUsersToColors.getOrDefault(userId, new Pair<String, Color>(null, Color.GRAY)).second();
					entriesForThisMonth.add(new Pair<>(color, entry.second().getExperience()));
					i++;
				}else {
					correctMonth = false;
				}
			}
			plotData.add(new Pair<>(position.getMonth() + " " + position.getYear(), new Plotter.Bar(entriesForThisMonth)));
		}
		
		BufferedImage plot = new Plotter(plotData, "General helper statistics").plot();
		try(ByteArrayOutputStream os = new ByteArrayOutputStream()){
			ImageIO.write(plot, "png", os);
			FileUpload upload = FileUpload.fromData(os.toByteArray(), "image.png");
			EmbedBuilder eb = new EmbedBuilder()
					.setTitle("Help XP distribution")
					.setDescription("This plot shows how much help XP have been awarded to different helpers.")
					.setImage("attachment://"+upload.getName());
			for (Entry<Long, Pair<String, Color>> entry : topUsersToColors.entrySet()) {
				eb.addField(entry.getValue().first(), "<@"+entry.getKey()+">", true);
			}
			event.getHook().sendMessageEmbeds(eb
					.build()).addFiles(upload).queue();
		} catch (IOException e) {
			ExceptionLogger.capture(e, "Cannot create XP plot");
			event.getHook().sendMessage("An error occured.").queue();
		}
	}
	
	private Map<Long, Pair<String, Color>> mapTopUsersToColors(List<Pair<MonthInYear,HelpAccount>> transactionWeights) {
		Map<Long, Double> totalByUser = new HashMap<>();
		for (Pair<MonthInYear,HelpAccount> pair : transactionWeights) {
			totalByUser.merge(pair.second().getUserId(), pair.second().getExperience(), (a,b) -> a+b);
		}
		
		PriorityQueue<Pair<Long, Double>> topUserQueue = new PriorityQueue<>(Comparator.comparingDouble(Pair::second));
		for (Entry<Long, Double> e : totalByUser.entrySet()) {
			topUserQueue.add(new Pair<>(e.getKey(), e.getValue()));
			if(topUserQueue.size()>COLORS.size()) {
				topUserQueue.remove();
			}
		}
		
		List<Long> topUsers = new ArrayList<>();
		while(!topUserQueue.isEmpty()) {
			topUsers.add(topUserQueue.remove().first());
		}
		Map<Long, Pair<String, Color>> topUsersToColors = new LinkedHashMap<>();
		for(int i=0;i<topUsers.size();i++) {
			topUsersToColors.put(topUsers.get(topUsers.size() - i - 1), COLORS.get(i));
		}
		return topUsersToColors;
	}
}
