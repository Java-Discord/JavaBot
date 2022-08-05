package net.javadiscord.javabot.data;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.Bot;
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
}
