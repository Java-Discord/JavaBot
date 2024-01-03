package net.discordjug.javabot;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import xyz.dynxsty.dih4jda.DIH4JDA;
import xyz.dynxsty.dih4jda.DIH4JDABuilder;
import xyz.dynxsty.dih4jda.exceptions.DIH4JDAException;
import xyz.dynxsty.dih4jda.interactions.commands.application.RegistrationType;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.annotations.PreRegisteredListener;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.SystemsConfig;
import net.discordjug.javabot.data.h2db.DbHelper;
import net.discordjug.javabot.tasks.PresenceUpdater;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 * This class holds all configuration settings and {@link Bean}s.
 */
@Configuration
@RequiredArgsConstructor
public class SpringConfig {
	@Bean
	PresenceUpdater standardActivityPresenceUpdater() {
		return PresenceUpdater.standardActivities();
	}

	@Bean
	DataSource dataSource(BotConfig config) {
		if (config.getSystems().getJdaBotToken().isEmpty())
			throw new RuntimeException("JDA Token not set. Stopping Bot...");
		return DbHelper.initDataSource(config);
	}

	@Bean
	ScheduledExecutorService asyncPool(BotConfig config) {
		return Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
	}

	@Bean
	SystemsConfig systemsConfig(BotConfig botConfig) {
		return botConfig.getSystems();
	}

	/**
	 * Initializes the {@link JDA} instances.
	 * @param botConfig the main configuration of the bot
	 * @param ctx the Spring application context used for obtaining all listeners
	 * @return the initialized {@link JDA} object
	 */
	@Bean
	JDA jda(BotConfig botConfig, ApplicationContext ctx) {
		Collection<Object> listeners = ctx.getBeansWithAnnotation(PreRegisteredListener.class).values();
		return JDABuilder.createDefault(botConfig.getSystems().getJdaBotToken())
			.setStatus(OnlineStatus.DO_NOT_DISTURB)
			.setChunkingFilter(ChunkingFilter.ALL)
			.setMemberCachePolicy(MemberCachePolicy.ALL)
			.enableCache(CacheFlag.ACTIVITY)
			.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)
			.addEventListeners(listeners.toArray())
			.build();
	}

	/**
	 * Initializes {@link DIH4JDA}.
	 * @param jda the initialized {@link JDA} instance
	 * @return the initialized {@link DIH4JDA} object
	 * @throws DIH4JDAException if an error occurs while initializing {@link DIH4JDA}
	 */
	@Bean
	DIH4JDA initializeDIH4JDA(JDA jda) throws DIH4JDAException {
		DIH4JDA.setDefaultRegistrationType(RegistrationType.GLOBAL);
		return DIH4JDABuilder.setJDA(jda)
			.setGlobalSmartQueue(false)
			.setGuildSmartQueue(false)
			.disableAutomaticCommandRegistration()
			.build();
	}

	@Bean
	BotConfig botConfig() {
		return new BotConfig(Path.of("config"));
	}
}
