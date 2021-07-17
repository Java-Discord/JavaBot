package com.javadiscord.javabot.events;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.other.Version;
import com.javadiscord.javabot.other.Misc;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class Startup extends ListenerAdapter {

    public static String iae = "https://cdn.discordapp.com/attachments/838019016395063328/838019031628906496/IllegalArgumentException.png";
    public static String mfurle = "https://cdn.discordapp.com/attachments/838020992882049025/838021012871315486/MalformedURLException.png";

    public static MongoClient mongoClient;
    public static SelfUser bot;
    public static Guild preferredGuild;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Startup.class);

    @Override
    public void onReady(ReadyEvent event){

        bot = event.getJDA().getSelfUser();

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

        try {
            StringBuilder sb = new StringBuilder();
            Scanner fReader = new Scanner(getClass().getClassLoader().getResourceAsStream("textfiles/startup.txt"));

            while (fReader.hasNextLine()) {
                sb.append(fReader.nextLine() + "\n");
            }
            System.out.println("\n" + sb.toString().replace("{!version}", new Version().getVersion()));

        } catch (Exception e) { logger.error("* textfiles/startup.txt not found"); }

        try { TimeUnit.MILLISECONDS.sleep(500); }
        catch (InterruptedException e) { e.printStackTrace(); }

        String[] guildOrder = new String[]{"648956210850299986", "675136900478140422", "861254598046777344"};
        //                                        Java              Mount Everest™    JavaDiscord Emoji Server

        for (int i = 0; i < event.getJDA().getGuilds().size(); i++) {

            try {
                preferredGuild = event.getJDA().getGuildById(guildOrder[i]);
                if (event.getJDA().getGuilds().contains(preferredGuild)) break;
                else continue;

            } catch (Exception ignored) {}
        }

        if (preferredGuild == null) preferredGuild = event.getJDA().getGuilds().get(0);

        logger.info("Bot is ready!");
        logger.info("Logged in as " + event.getJDA().getSelfUser().getAsTag());

        logger.info("Preferred Guild: " + preferredGuild.getName());
        logger.info("Guilds: " + Misc.getGuildList(event.getJDA().getGuilds(), true, true));

        new StarboardListener().updateAllSBM(event);

        for (var guild : event.getJDA().getGuilds()) {
            Bot.slashCommands.registerSlashCommands(guild);
        }
    }
}
