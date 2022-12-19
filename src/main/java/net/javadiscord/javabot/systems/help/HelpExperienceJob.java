package net.javadiscord.javabot.systems.help;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

/**
 * Removes a specified amount of experience from everyone's help account.
 */
@Service
@RequiredArgsConstructor
public class HelpExperienceJob {
	private final JDA jda;
	private final BotConfig botConfig;
	private final ExecutorService asyncPool;
	private final HelpAccountRepository helpAccountRepository;

	/**
	 * Removes a specified amount of experience from everyone's help account.
	 */
	@Scheduled(cron = "0 0 0 * * *") // Daily, 00:00 UTC
	public void execute() {
		asyncPool.execute(() -> {
			try {
				helpAccountRepository.removeExperienceFromAllAccounts(
						// just get the config for the first guild the bot is in, as it's not designed to work in multiple guilds anyway
						botConfig.get(jda.getGuilds().get(0)).getHelpConfig().getDailyExperienceSubtraction(), 5, 50);
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, DbHelper.class.getSimpleName());
			}
		});
	}
}
