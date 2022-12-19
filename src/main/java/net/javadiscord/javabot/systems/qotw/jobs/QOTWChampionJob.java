package net.javadiscord.javabot.systems.qotw.jobs;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;

/**
 * Jobs which gives the QOTW Champion role to the users with the highest monthly QOTW score.
 */
@RequiredArgsConstructor
@Service
public class QOTWChampionJob {

	private final BotConfig botConfig;
	private final QOTWPointsService pointsService;
	private final QuestionPointsRepository pointsRepository;
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
				pointsRepository.getTopAccounts(month, 0, 1)
				.stream()
				.findFirst()
				.ifPresent(best -> {
					for (Member member : guild.getMembersWithRoles(qotwChampionRole)) {
						if (pointsService.getOrCreateAccount(member.getIdLong()).getPoints() < best.getPoints()) {
							member.getGuild().removeRoleFromMember(member, qotwChampionRole).queue();
						}
					}
					for (Long userId : pointsRepository.getUsersWithSpecificScore(month, best.getPoints())) {
						Member member = guild.getMemberById(userId);
						if(member != null) {
							guild.addRoleToMember(member, qotwChampionRole).queue();
						}
					}
				});
			}
		}
	}

}
