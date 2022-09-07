package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Removes a specified amount of experience from everyone's help account.
 */
@Service
@RequiredArgsConstructor
public class HelpExperienceJob {
	private final JDA jda;
	private final BotConfig botConfig;
	private final DbHelper dbHelper;

	/**
	 * Removes a specified amount of experience from everyone's help account.
	 */
	@Scheduled(cron = "0 0 0 * * *")//daily 00:00
	public void execute() {
		dbHelper.doDaoAction(HelpAccountRepository::new, dao -> dao.removeExperienceFromAllAccounts(
				// just get the config for the first guild the bot is in, as it's not designed to work in multiple guilds anyway
				botConfig.get(jda.getGuilds().get(0)).getHelpConfig().getDailyExperienceSubtraction(), 100, 100)
		);
	}
}
