package net.javadiscord.javabot;

import com.zaxxer.hikari.HikariDataSource;
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
import net.javadiscord.javabot.command.InteractionHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.data.h2db.message_cache.MessageCache;
import net.javadiscord.javabot.data.h2db.message_cache.MessageCacheListener;
import net.javadiscord.javabot.listener.*;
import net.javadiscord.javabot.systems.help.HelpChannelListener;
import net.javadiscord.javabot.systems.moderation.AutoMod;
import net.javadiscord.javabot.systems.moderation.ServerLock;
import net.javadiscord.javabot.systems.starboard.StarboardManager;
import net.javadiscord.javabot.tasks.PresenceUpdater;
import net.javadiscord.javabot.tasks.ScheduledTasks;
import net.javadiscord.javabot.tasks.StatsUpdater;
import net.javadiscord.javabot.util.ImageCacheUtils;
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
	 * A reference to the slash command listener that's the main point of
	 * interaction for users with this bot. It's marked as a publicly accessible
	 * reference so that {@link InteractionHandler#registerCommands} can
	 * be called wherever it's needed.
	 */
	public static InteractionHandler interactionHandler;
	/**
	 * An instance of {@link AutoMod}.
	 * */
	public static AutoMod autoMod;
	/**
	 * The Bots {@link MessageCache}, which handles logging of deleted and edited messages.
	 */
	public static MessageCache messageCache;
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
	/**
	 * A reference to the Bot's {@link ImageCacheUtils}.
	 */
	public static ImageCacheUtils imageCache;

	private Bot() {
	}

	/**
	 * The main method that starts the bot. This involves a few steps:
	 * <ol>
	 *     <li>Setting the time zone to UTC, to keep our sanity when working with times.</li>
	 *     <li>Loading the configuration JSON file.</li>
	 *     <li>Initializing the {@link InteractionHandler} listener (which reads command data from a YAML file).</li>
	 *     <li>Creating and configuring the {@link JDA} instance that enables the bot's Discord connectivity.</li>
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
		interactionHandler = new InteractionHandler();
		messageCache = new MessageCache();
		autoMod = new AutoMod();
		imageCache = new ImageCacheUtils();
		asyncPool = Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
		var jda = JDABuilder.createDefault(config.getSystems().getJdaBotToken())
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.setChunkingFilter(ChunkingFilter.ALL)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableCache(CacheFlag.ACTIVITY)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
				.addEventListeners(interactionHandler, autoMod)
				.build();
		AllowedMentions.setDefaultMentions(EnumSet.of(Message.MentionType.ROLE, Message.MentionType.CHANNEL, Message.MentionType.USER, Message.MentionType.EMOTE));
		addEventListeners(jda);
		try {
			ScheduledTasks.init(jda);
			log.info("Initialized scheduled tasks.");
		} catch (SchedulerException e) {
			log.error("Could not initialize all scheduled tasks.", e);
			jda.shutdown();
		}
	}

	/**
	 * Adds all the bot's event listeners to the JDA instance, except for the
	 * main {@link InteractionHandler} listener and {@link AutoMod}.
	 *
	 * @param jda The JDA bot instance to add listeners to.
	 */
	private static void addEventListeners(JDA jda) {
		jda.addEventListener(
				new MessageCacheListener(),
				new GitHubLinkListener(),
				new MessageLinkListener(),
				new GuildJoinListener(),
				new ServerLock(jda),
				new UserLeaveListener(),
				new StartupListener(),
				PresenceUpdater.standardActivities(),
				new StatsUpdater(),
				new SuggestionListener(),
				new StarboardManager(),
				new InteractionListener(),
				new HelpChannelListener(),
				new ShareKnowledgeVoteListener(),
				new JobChannelVoteListener(),
				new PingableNameListener()
		);
	}
}

