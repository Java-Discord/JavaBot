package net.discordjug.javabot.systems.qotw.jobs;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.dao.QOTWChampionRepository;
import net.discordjug.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

/**
 * Jobs which gives the QOTW Champion role to the users with the highest monthly QOTW score.
 */
@RequiredArgsConstructor
@Service
public class QOTWChampionJob {

	private final BotConfig botConfig;
	private final QOTWPointsService pointsService;
	private final QuestionPointsRepository pointsRepository;
	private final QOTWChampionRepository qotwChampionRepository;
	private final JDA jda;

	/**
	 * Gives the QOTW Champion role to the users with the highest monthly QOTW score.
	 */
	@Scheduled(cron = "0 0 0 1 * *") //start of month
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			LocalDate month=LocalDate.now().minusMonths(1);
			Role qotwChampionRole = botConfig.get(guild).getQotwConfig().getQOTWChampionRole();
			if (qotwChampionRole != null) {
				pointsRepository.getTopAccounts(month, 1, 1)
				.stream()
				.findFirst()
				.ifPresent(best -> {
					guild
						.retrieveMembersByIds(qotwChampionRepository.getCurrentQOTWChampions(guild.getIdLong()))
						.onSuccess(membersToRemove -> {
							for (Member member : membersToRemove) {
								if (pointsService.getOrCreateAccount(member.getIdLong()).getPoints() < best.getPoints()) {
									member.getGuild().removeRoleFromMember(member, qotwChampionRole).queue();
								}
							}
							guild
								.retrieveMembersByIds(
									pointsRepository.getUsersWithSpecificScore(month, best.getPoints())
								).onSuccess(membersToAdd -> {
									for (Member member : membersToAdd) {
										guild.addRoleToMember(member, qotwChampionRole).queue();
									}
								});
						});
				});
			}
		}
	}

}
