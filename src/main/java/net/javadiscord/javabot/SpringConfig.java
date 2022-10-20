package net.javadiscord.javabot;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.security.auth.login.LoginException;
import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dynxsty.dih4jda.DIH4JDA;
import com.dynxsty.dih4jda.DIH4JDABuilder;
import com.dynxsty.dih4jda.DIH4JDALogger;
import com.dynxsty.dih4jda.exceptions.DIH4JDAException;
import com.dynxsty.dih4jda.interactions.commands.RegistrationType;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.javadiscord.javabot.annotations.PreRegisteredListener;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.SystemsConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.tasks.PresenceUpdater;

/**
 * This class holds all configuration settings and {@link Bean}s.
 */
@Configuration
@RequiredArgsConstructor
public class SpringConfig {
	@Bean
	public PresenceUpdater standardActivityPresenceUpdater() {
		return PresenceUpdater.standardActivities();
	}

	@Bean
	public DataSource getDataSource(BotConfig config) {
		return DbHelper.initDataSource(config);
	}

	@Bean
	public ScheduledExecutorService asyncPool(BotConfig config) {
		return Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
	}

	@Bean
	public BotConfig config() {
		return new BotConfig(Path.of("config"));
	}

	@Bean
	public SystemsConfig systemsConfig(BotConfig botConfig) {
		return botConfig.getSystems();
	}

	/**
	 * Initializes the {@link JDA} instances.
	 * @param botConfig the main configuration of the bot
	 * @param ctx the Spring application context used for obtaining all listeners
	 * @return the initialized {@link JDA} object
	 * @throws LoginException if the token is invalid
	 */
	@Bean
	public JDA jda(BotConfig botConfig, ApplicationContext ctx) throws LoginException {
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
	public DIH4JDA initializeDIH4JDA(JDA jda) throws DIH4JDAException {
		return DIH4JDABuilder.setJDA(jda)
			.setDefaultCommandType(RegistrationType.GLOBAL)
			.disableLogging(DIH4JDALogger.Type.SMART_QUEUE_IGNORED)
			.disableAutomaticCommandRegistration()
			.build();
	}

	@Bean
	public BotConfig botConfig() {
		return new BotConfig(Path.of("config"));
	}
}
