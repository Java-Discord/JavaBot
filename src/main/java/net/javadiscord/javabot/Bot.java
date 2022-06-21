package net.javadiscord.javabot;

import com.dynxsty.dih4jda.DIH4JDA;
import com.dynxsty.dih4jda.DIH4JDABuilder;
import com.dynxsty.dih4jda.interactions.commands.RegistrationType;
import com.zaxxer.hikari.HikariDataSource;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.data.h2db.message_cache.MessageCache;
import net.javadiscord.javabot.data.h2db.message_cache.MessageCacheListener;
import net.javadiscord.javabot.listener.*;
import net.javadiscord.javabot.systems.custom_commands.CustomCommandManager;
import net.javadiscord.javabot.systems.help.HelpChannelListener;
import net.javadiscord.javabot.systems.moderation.AutoMod;
import net.javadiscord.javabot.systems.moderation.server_lock.ServerLock;
import net.javadiscord.javabot.systems.starboard.StarboardManager;
import net.javadiscord.javabot.tasks.PresenceUpdater;
import net.javadiscord.javabot.tasks.ScheduledTasks;
import net.javadiscord.javabot.tasks.StatsUpdater;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.ImageCache;
import org.quartz.SchedulerException;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The main class where the bot is initialized.
 */
@Slf4j
public class Bot {

	/**
	 * The set of configuration properties that this bot uses.
	 */
	public static BotConfig config;

	/**
	 * An instance of {@link AutoMod}.
	 */
	public static AutoMod autoMod;

	/**
	 * A reference to the Bot's {@link DIH4JDA}.
	 */
	public static DIH4JDA dih4jda;

	/**
	 * The Bots {@link MessageCache}, which handles logging of deleted and edited messages.
	 */
	public static MessageCache messageCache;

	/**
	 * A reference to the Bot's {@link ImageCache}.
	 */
	public static ImageCache imageCache;

	/**
	 * A reference to the bot's serverlock.
	 */
	public static ServerLock serverLock;

	/**
	 * A static reference to the {@link CustomCommandManager} which handles and loads all registered Custom Commands.
	 */
	public static CustomCommandManager customCommandManager;

	/**
	 * A reference to the data source that provides access to the relational
	 * database that this bot users for certain parts of the application. Use
	 * this to obtain a connection and perform transactions.
	 */
	public static HikariDataSource dataSource;

	/**
	 * A general-purpose thread pool that can be used by the bot to execute
	 * tasks outside the main event processing thread.
	 */
	public static ScheduledExecutorService asyncPool;

	private Bot() {
	}

	/**
	 * The main method that starts the bot. This involves a few steps:
	 * <ol>
	 *     <li>Setting the time zone to UTC, to keep our sanity when working with times.</li>
	 *     <li>Loading the configuration JSON file.</li>
	 *     <li>Creating and configuring the {@link JDA} instance that enables the bot's Discord connectivity.</li>
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
		messageCache = new MessageCache();
		autoMod = new AutoMod();
		imageCache = new ImageCache();
		asyncPool = Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
		JDA jda = JDABuilder.createDefault(config.getSystems().getJdaBotToken())
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.setChunkingFilter(ChunkingFilter.ALL)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableCache(CacheFlag.ACTIVITY)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
				.addEventListeners(autoMod)
				.build();
		AllowedMentions.setDefaultMentions(EnumSet.of(Message.MentionType.ROLE, Message.MentionType.CHANNEL, Message.MentionType.USER, Message.MentionType.EMOTE));
		dih4jda = DIH4JDABuilder.setJDA(jda)
				.setCommandsPackage("net.javadiscord.javabot")
				.setDefaultCommandType(RegistrationType.GUILD)
				.build();
		serverLock = new ServerLock(jda);
		customCommandManager = new CustomCommandManager(jda, dataSource);
		addEventListeners(jda, dih4jda);
		// initialize Sentry
		Sentry.init(options -> {
			options.setDsn(config.getSystems().getSentryDsn());
			options.setTracesSampleRate(1.0);
			options.setDebug(false);
		});
		try {
			ScheduledTasks.init(jda);
			log.info("Initialized scheduled tasks.");
		} catch (SchedulerException e) {
			ExceptionLogger.capture(e, Bot.class.getSimpleName());
			log.error("Could not initialize all scheduled tasks.", e);
			jda.shutdown();
		}
	}

	/**
	 * Adds all the bot's event listeners to the JDA instance, except for
	 * the {@link AutoMod} instance.
	 *
	 * @param jda     The JDA bot instance to add listeners to.
	 * @param dih4jda The {@link DIH4JDA} instance.
	 */
	private static void addEventListeners(JDA jda, DIH4JDA dih4jda) {
		jda.addEventListener(
				customCommandManager,
				serverLock,
				PresenceUpdater.standardActivities(),
				new MessageCacheListener(),
				new GitHubLinkListener(),
				new MessageLinkListener(),
				new GuildJoinListener(),
				new UserLeaveListener(),
				new StateListener(),
				new StatsUpdater(),
				new SuggestionListener(),
				new StarboardManager(),
				new HelpChannelListener(),
				new ShareKnowledgeVoteListener(),
				new JobChannelVoteListener(),
				new PingableNameListener()
		);
		dih4jda.addListener(new DIH4JDAListener());
	}
}

