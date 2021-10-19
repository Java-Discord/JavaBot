package com.javadiscord.javabot;

import com.javadiscord.javabot.commands.SlashCommands;
import com.javadiscord.javabot.data.h2db.H2DataSource;
import com.javadiscord.javabot.events.*;
import com.javadiscord.javabot.service.AutoMod;
import com.javadiscord.javabot.service.PresenceUpdater;
import com.javadiscord.javabot.service.Startup;
import com.javadiscord.javabot.service.StatsUpdater;
import com.javadiscord.javabot.service.help.HelpChannelListener;
import com.javadiscord.javabot.data.properties.config.BotConfig;
import com.javadiscord.javabot.service.welcome.UserJoin;
import com.javadiscord.javabot.service.welcome.UserLeave;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The main class where the bot is initialized.
 */
public class Bot {
    /**
     * A reference to the JDA instance
     */
    public static JDA jda;

    /**
     * The set of configuration properties that this bot uses.
     */
    public static BotConfig config;

    /**
     * A reference to the slash command listener that's the main point of
     * interaction for users with this bot. It's marked as a publicly accessible
     * reference so that {@link SlashCommands#registerSlashCommands(Guild)} can
     * be called wherever it's needed.
     */
    public static SlashCommands slashCommands;

    /**
     * A reference to the data source that provides access to the relational
     * database that this bot users for certain parts of the application. Use
     * this to obtain a connection and perform transactions.
     */
    public static H2DataSource dataSource;

    /**
     * A general-purpose thread pool that can be used by the bot to execute
     * tasks outside the main event processing thread.
     */
    public static ScheduledExecutorService asyncPool;

    /**
     * The main method that starts the bot. This involves a few steps:
     * <ol>
     *     <li>Setting the time zone to UTC, to keep our sanity when working with times.</li>
     *     <li>Loading the configuration JSON file.</li>
     *     <li>Initializing the {@link SlashCommands} listener (which reads command data from a YAML file).</li>
     *     <li>Creating and configuring the {@link JDA} instance that enables the bot's Discord connectivity.</li>
     *     <li>Adding event listeners to the bot.</li>
     * </ol>
     * @param args Command-line arguments.
     * @throws Exception If any exception occurs during bot creation.
     */
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
        config = new BotConfig(Path.of("config"));
        slashCommands = new SlashCommands();
        dataSource = new H2DataSource();
        dataSource.initDatabase();
        asyncPool = Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
        jda = JDABuilder.createDefault(config.getSystems().getJdaBotToken())
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.ACTIVITY)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(slashCommands)
                .build();
        addEventListeners(jda);
    }

    /**
     * Adds all the bot's event listeners to the JDA instance, except for the
     * main {@link SlashCommands} listener.
     * @param jda The JDA bot instance to add listeners to.
     */
    private static void addEventListeners(JDA jda) {
        jda.addEventListener(
                new GuildJoinListener(),
                new UserJoin(),
                new UserLeave(),
                new Startup(),
                PresenceUpdater.standardActivities(),
                new StatsUpdater(),
                new SuggestionListener(),
                new AutoMod(),
                new SubmissionListener(),
                new StarboardListener(),
                new InteractionListener(),
                new HelpChannelListener(),
                new ShareKnowledgeVoteListener()
        );
    }
}

