package net.javadiscord.javabot;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.tasks.PresenceUpdater;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class holds all configuration settings and {@link Bean}s.
 */
@Configuration
public class SpringConfig {
	@Bean
	public JDA getJDA() {
		return Bot.getDih4jda().getJDA();
	}

	@Bean
	public PresenceUpdater standardActivityPresenceUpdater() {
		return PresenceUpdater.standardActivities();
	}

	@Bean
	public DataSource getDataSource() {
		return Bot.getDataSource();
	}
}
