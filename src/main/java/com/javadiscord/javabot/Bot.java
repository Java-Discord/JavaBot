package com.javadiscord.javabot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.javadiscord.javabot.events.*;
import com.javadiscord.javabot.properties.MultiProperties;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;


public class Bot {
    private static final Properties properties = new MultiProperties(Path.of("bot.props"));

    public static void main(String[] args) throws Exception {
            CommandClient client = new CommandClientBuilder()
                    .setOwnerId("374328434677121036")
                    .setCoOwnerIds("299555811804315648", "620615131256061972", "810481402390118400")
                    .setPrefix("!")
                    .setEmojis("✅", "⚠️", "❌")
                    .useHelpBuilder(false)
                    .addCommands(discoverCommands())
                    .build();

            JDA jda = JDABuilder.createDefault(properties.getProperty("token", "null"))
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.ACTIVITY)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                    .addEventListeners(client, new SlashCommands(client))
                    .build();

            //EVENTS
            jda.addEventListener(new GuildJoin());
            jda.addEventListener(new UserJoin());
            jda.addEventListener(new UserLeave());
            jda.addEventListener(new Startup());
            jda.addEventListener(new StatusUpdate());
            jda.addEventListener(new ReactionListener());
            jda.addEventListener(new SuggestionListener());
            jda.addEventListener(new CstmCmdListener());
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

    /**
     * Discovers and instantiates all commands found in the bot's "commands"
     * package. This uses the reflections API to find all classes in that
     * package which extend from the base {@link Command} class.
     * <p>
     *     <strong>All command classes MUST have a no-args constructor.</strong>
     * </p>
     * @return The array of commands.
     */
    private static Command[] discoverCommands() {
        Reflections reflections = new Reflections("com.javadiscord.javabot.commands");
        return reflections.getSubTypesOf(Command.class).stream()
            .map(type -> {
                try {
                    if (Modifier.isAbstract(type.getModifiers())) return null;
                    return (Command) type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toArray(Command[]::new);
    }
}

