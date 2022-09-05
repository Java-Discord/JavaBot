package net.javadiscord.javabot;

import com.dynxsty.dih4jda.DIH4JDA;
import com.dynxsty.dih4jda.DIH4JDABuilder;
import com.dynxsty.dih4jda.DIH4JDALogger;
import com.dynxsty.dih4jda.interactions.commands.ContextCommand;
import com.dynxsty.dih4jda.interactions.commands.RegistrationType;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;
import com.dynxsty.dih4jda.interactions.components.ModalHandler;
import com.dynxsty.dih4jda.interactions.components.SelectMenuHandler;
import com.zaxxer.hikari.HikariDataSource;
import io.sentry.Sentry;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.javadiscord.javabot.tasks.PresenceUpdater;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;

/**
 * The main class where the bot is initialized.
 */
@SpringBootApplication
@ComponentScan(
	includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SlashCommand.class, ContextCommand.class, ListenerAdapter.class }),
	excludeFilters = @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, classes = PresenceUpdater.class)
)
@EnableScheduling
public class Bot {

	@Getter
	private static BotConfig config;

	@Getter
	private static DIH4JDA dih4jda;

	@Getter
	private static HikariDataSource dataSource;

	@Getter
	private static ScheduledExecutorService asyncPool;

	/**
	 * The constructor of this class, which also adds all {@link SlashCommand} and
	 * {@link ContextCommand} to the {@link DIH4JDA} instance.
	 *
	 * @param commands The {@link Autowired} list of {@link SlashCommand}s.
	 * @param contexts The {@link Autowired} list of {@link ContextCommand}s.
	 * @param listeners The {@link Autowired} list of {@link ListenerAdapter}s.
	 */
	@Autowired
	public Bot(final List<SlashCommand> commands, final List<ContextCommand> contexts, final List<ListenerAdapter> listeners) {
		if (!commands.isEmpty()) {
			getDih4jda().addSlashCommands(commands.toArray(SlashCommand[]::new));
		}
		if (!contexts.isEmpty()) {
			getDih4jda().addContextCommands(contexts.toArray(ContextCommand[]::new));
		}
		try {
			getDih4jda().registerInteractions();
		} catch (ReflectiveOperationException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
		addEventListeners(listeners);
	}

	private void addEventListeners(final List<ListenerAdapter> listeners) {
		for (ListenerAdapter listener : listeners) {
			dih4jda.getJDA().addEventListener(listener);
		}
		dih4jda.getJDA().addEventListener(dih4jda);
	}

	/**
	 * Initialize Sentry.
	 */
	@PostConstruct
	public void init() {
		Sentry.init(options -> {
			options.setDsn(config.getSystems().getSentryDsn());
			options.setTracesSampleRate(1.0);
			options.setDebug(false);
		});
	}

	/**
	 * The main method that starts the bot. This involves a few steps:
	 * <ol>
	 *     <li>Setting the time zone to UTC, to keep our sanity when working with times.</li>
	 *     <li>Loading the configuration JSON file.</li>
	 *     <li>Creating and configuring the {@link JDA} instance that enables the bots' Discord connectivity.</li>
	 *     <li>Initializing the {@link DIH4JDA} instance.</li>
	 *     <li>Adding event listeners to the bot.</li>
	 * </ol>
	 *
	 * @param args Command-line arguments.
	 * @throws Exception If any exception occurs during bot creation.
	 */
	public static void main(String[] args) throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
		config = new BotConfig(Path.of("config"));
		dataSource = DbHelper.initDataSource(config);
		asyncPool = Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
		JDA jda = JDABuilder.createDefault(config.getSystems().getJdaBotToken())
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.setChunkingFilter(ChunkingFilter.ALL)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableCache(CacheFlag.ACTIVITY)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)
				.build();
		AllowedMentions.setDefaultMentions(EnumSet.of(Message.MentionType.ROLE, Message.MentionType.CHANNEL, Message.MentionType.USER, Message.MentionType.EMOJI));
		dih4jda = DIH4JDABuilder.setJDA(jda)
				.setDefaultCommandType(RegistrationType.GLOBAL)
				.disableLogging(DIH4JDALogger.Type.SMART_QUEUE_IGNORED)
				.disableAutomaticCommandRegistration()
				.build();
		ConfigurableApplicationContext ctx = SpringApplication.run(Bot.class, args);
		registerComponentHandlers(ctx);

	}

	private static void registerComponentHandlers(ConfigurableApplicationContext ctx) {
		Map<String, Object> interactionHandlers = ctx.getBeansWithAnnotation(AutoDetectableComponentHandler.class);
		Map<List<String>, ButtonHandler> buttonHandlers = new HashMap<>();
		Map<List<String>, ModalHandler> modalHandlers = new HashMap<>();
		Map<List<String>, SelectMenuHandler> selectMenuHandlers = new HashMap<>();
		for (Object handler : interactionHandlers.values()) {
			AutoDetectableComponentHandler annotation = handler.getClass().getAnnotation(AutoDetectableComponentHandler.class);
			List<String> keys = Arrays.asList(annotation.value());
			addHandler(buttonHandlers, keys, handler, ButtonHandler.class);
			addHandler(modalHandlers, keys, handler, ModalHandler.class);
			addHandler(selectMenuHandlers, keys, handler, SelectMenuHandler.class);
		}
		dih4jda.addButtonHandlers(buttonHandlers);
		dih4jda.addModalHandlers(modalHandlers);
		dih4jda.addSelectMenuHandlers(selectMenuHandlers);
	}

	private static <T> void addHandler(Map<List<String>, T> handlers, List<String> keys, Object handler, Class<T> type) {
		if (type.isInstance(handler)) {
			T old = handlers.putIfAbsent(keys, type.cast(handler));
			if(old!=null) {
				throw new IllegalStateException("The same interaction name was registered multiple times");
			}
		}
	}
}

