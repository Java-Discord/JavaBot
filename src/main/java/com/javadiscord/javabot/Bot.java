package com.javadiscord.javabot;

import com.javadiscord.javabot.data.H2DataSource;
import com.javadiscord.javabot.events.*;
import com.javadiscord.javabot.properties.MultiProperties;
import com.javadiscord.javabot.properties.config.BotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The main class where the bot is initialized.
 */
public class Bot {
    /**
     * Loads the bot properties, first from the internal classpath properties
     * file, and then any properties file in the current working directory will
     * take precedence over that.
     */
    private static final Properties properties = new MultiProperties(
        MultiProperties.getClasspathResource("bot.properties").orElseThrow(),
        Path.of("bot.props")
    );

    private static BotConfig config;

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
     *     <li>Initializing the {@link SlashCommands} listener (which reads command data from a YAML file).</li>
     *     <li>Creating and configuring the {@link JDA} instance that enables the bot's Discord connectivity.</li>
     *     <li>Adding event listeners to the bot.</li>
     * </ol>
     * @param args Command-line arguments.
     * @throws Exception If any exception occurs during bot creation.
     */
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));

        try {
            config = BotConfig.load(Path.of("config.json"));
        } catch (IOException e) {
            config = new BotConfig(Path.of("config.json"));
            config.save();
        }
        slashCommands = new SlashCommands();
        dataSource = new H2DataSource();
        dataSource.initDatabase();
        asyncPool = Executors.newScheduledThreadPool(Integer.parseInt(getProperty("asyncPoolSize")));
        JDA jda = JDABuilder.createDefault(properties.getProperty("token", "null"))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
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
                new GuildJoin(),
                new UserJoin(),
                new UserLeave(),
                new Startup(),
                PresenceUpdater.standardActivities(),
                new SuggestionListener(),
                new AutoMod(),
                new SubmissionListener(),
                new StarboardListener(),
                new ButtonClickListener()
        );
    }

    /**
     * Gets the value of a property from the bot's loaded properties.
     * @see Properties#getProperty(String)
     * @param key The name of the property to get.
     * @return The value of the property, or <code>null</code> if none was found.
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets the value of a property from the bot's loaded properties.
     * @see Properties#getProperty(String, String)
     * @param key The name of the property to get.
     * @param defaultValue The value to return if no property was found.
     * @return The value of the property, or the default value.
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}

