package com.javadiscord.javabot.events;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.other.Version;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Misc;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Startup extends ListenerAdapter {

    public static String iae = "https://cdn.discordapp.com/attachments/838019016395063328/838019031628906496/IllegalArgumentException.png";
    public static String mfurle = "https://cdn.discordapp.com/attachments/838020992882049025/838021012871315486/MalformedURLException.png";

    public static MongoClient mongoClient;
    public static Guild preferredGuild;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Startup.class);

    @Override
    public void onReady(ReadyEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.ERROR);

        try {
            MongoClientURI uri = new MongoClientURI(Bot.getProperty("mongologin", "default"));
            mongoClient = new MongoClient(uri);

            logger.info("Successfully connected to Database");
        } catch (Exception e) {
            logger.error("Couldn't connect to Database... Shutting down...");
            System.exit(0);
        }

        try (BufferedReader startupMsgReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("textfiles/startup.txt")))) {
			String startup = startupMsgReader
                    .lines()
                    .collect(Collectors.joining("\n"))
                    .replace("{!version}", new Version().getVersion());
            System.out.println("\n" + startup);
        } catch (Exception e) { logger.error("* textfiles/startup.txt not found"); }

        try { TimeUnit.MILLISECONDS.sleep(500); }
        catch (InterruptedException e) { e.printStackTrace(); }

        String[] guildOrder = new String[] {"648956210850299986", "675136900478140422", "861254598046777344"};
        //                                         Java              Mount Everest™    JavaDiscord Emoji Server

        JDA jda = event.getJDA();
        List<Guild> jdaGuilds = jda.getGuilds();
        preferredGuild = Arrays.stream(guildOrder)
                .map(jda::getGuildById)
                .filter(jdaGuilds::contains)
                .findFirst()
                .orElse(jdaGuilds.get(0));

        logger.info("Bot is ready!");
        logger.info("Logged in as " + event.getJDA().getSelfUser().getAsTag());

        logger.info("Preferred Guild: " + preferredGuild.getName());
        logger.info("Guilds: " + Misc.getGuildList(event.getJDA().getGuilds(), true, true));

        List<String> skipGuilds = List.of("861254598046777344", "813817075218776101");
        //                                JavaDiscord Emoji Server    Test-Server
        for (var guild : jdaGuilds) {
            if (skipGuilds.contains(guild.getId())) continue;

            Database.deleteOpenSubmissions(guild);
            new StarboardListener().updateAllSBM(event, guild);
            Bot.slashCommands.registerSlashCommands(guild);
        }
    }
}
