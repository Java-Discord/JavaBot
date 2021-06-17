package com.javadiscord.javabot;

import com.javadiscord.javabot.events.*;
import com.javadiscord.javabot.properties.MultiProperties;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.file.Path;
import java.util.Properties;

public class Bot {
    private static final Properties properties = new MultiProperties(
        MultiProperties.getClasspathResource("bot.properties").orElseThrow(),
        Path.of("bot.props")

    );

    public static SlashCommands slashCommands;

    public static void main(String[] args) throws Exception {
        slashCommands = new SlashCommands();

        JDA jda = JDABuilder.createDefault(properties.getProperty("token", "null"))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.ACTIVITY)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(slashCommands)
                .build();

        //EVENTS
        jda.addEventListener(new GuildJoin());
        jda.addEventListener(new UserJoin());
        jda.addEventListener(new UserLeave());
        jda.addEventListener(new Startup());
        jda.addEventListener(PresenceUpdater.standardActivities());
        jda.addEventListener(new ReactionListener());
        jda.addEventListener(new SuggestionListener());
        jda.addEventListener(new AutoMod());
        jda.addEventListener(new SubmissionListener());
        //jda.addEventListener(new StarboardListener());
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

