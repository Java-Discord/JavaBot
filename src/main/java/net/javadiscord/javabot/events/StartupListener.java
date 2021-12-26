package net.javadiscord.javabot.events;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.data.mongodb.Database;
import net.javadiscord.javabot.systems.help.HelpChannelUpdater;
import net.javadiscord.javabot.systems.help.checks.SimpleGreetingCheck;
import net.javadiscord.javabot.util.Misc;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StartupListener extends ListenerAdapter {
    public static MongoClient mongoClient;
    public static Guild preferredGuild;

    @Override
    public void onReady(ReadyEvent event) {
        // Initialize all guild-specific configuration.
        Bot.config.loadGuilds(event.getJDA().getGuilds());
        Bot.config.flush();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.ERROR);

        try { TimeUnit.MILLISECONDS.sleep(500); }
        catch (InterruptedException e) { e.printStackTrace(); }

        String[] guildOrder = new String[]{"648956210850299986", "675136900478140422", "861254598046777344"};
        //                                        Java              Mount Everest™    JavaDiscord Emoji Server

        for (int i = 0; i < event.getJDA().getGuilds().size(); i++) {

            try {
                preferredGuild = event.getJDA().getGuildById(guildOrder[i]);
                if (event.getJDA().getGuilds().contains(preferredGuild)) break;

            } catch (Exception ignored) {}
        }

        if (preferredGuild == null) preferredGuild = event.getJDA().getGuilds().get(0);

        log.info("Logged in as {}{}{}",
                Constants.TEXT_WHITE, event.getJDA().getSelfUser().getAsTag(), Constants.TEXT_RESET);
        log.info("Preferred Guild: {}{}{}",
                Constants.TEXT_WHITE, preferredGuild.getName(), Constants.TEXT_RESET);
        log.info("Guilds: " + Misc.getGuildList(event.getJDA().getGuilds(), true, true));

        String[] skipGuilds = new String[]{"861254598046777344", "813817075218776101"};
        //                               JavaDiscord Emoji Server    Test-Server

        try {

        MongoClientURI uri = new MongoClientURI(Bot.config.getSystems().getMongoDatabaseUrl());
        mongoClient = new MongoClient(uri);

        new Database().databaseCheck(mongoClient, event.getJDA().getGuilds());
        log.info("Successfully connected to MongoDB");

        log.info("Starting Guild initialization\n");
        for (var guild : event.getJDA().getGuilds()) {
            if (Arrays.asList(skipGuilds).contains(guild.getId())) continue;

            new StarboardListener().updateAllSBM(guild);
            Bot.slashCommands.registerSlashCommands(guild);

            // Schedule the help channel updater to run periodically for each guild.
            var helpConfig = Bot.config.get(guild).getHelp();
            Bot.asyncPool.scheduleAtFixedRate(
                    new HelpChannelUpdater(event.getJDA(), helpConfig, List.of(
                            new SimpleGreetingCheck()
                    )),
                    5,
                    helpConfig.getUpdateIntervalSeconds(),
                    TimeUnit.SECONDS
            );

            Bot.config.get(guild).getModeration().getLogChannel().sendMessage("I have just been booted up!").queue();
        }

        } catch (MongoException e) {

            log.error("Couldn't connect to MongoDB ({}) Shutting down...", e.getClass().getSimpleName());
            e.printStackTrace();
            System.exit(0);
        }
    }
}
