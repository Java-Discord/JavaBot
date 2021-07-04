package com.javadiscord.javabot.events;

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

    @Override
    public void onReady(ReadyEvent event){

        bot = event.getJDA().getSelfUser();

        try {
            MongoClientURI uri = new MongoClientURI(Bot.getProperty("mongologin", "default"));
            mongoClient = new MongoClient(uri);

            LoggerFactory.getLogger(Startup.class).info("* Successfully connected to Database!");

        } catch(Exception e) {
            LoggerFactory.getLogger(Startup.class).error("* Couldn't connect to Database... Shutting down...");
            System.exit(0);
        }

        try {
            StringBuilder sb = new StringBuilder();
            Scanner fReader = new Scanner(getClass().getClassLoader().getResourceAsStream("textfiles/startup.txt"));

            while (fReader.hasNextLine()) {
                sb.append(fReader.nextLine() + "\n");
            }
            System.out.println("\n" + sb.toString().replace("{!version}", new Version().getVersion()));

        } catch (Exception e) {
            LoggerFactory.getLogger(Startup.class).error("* textfiles/startup.txt not found");
        }

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String[] guildOrder = new String[]{"648956210850299986", "675136900478140422", "861254598046777344"};
        //                                        Java              Mount Everest™    JavaDiscord Emoji Server

        for (int i = 0; i < event.getJDA().getGuilds().size(); i++) {

            try { preferredGuild = event.getJDA().getGuildById(guildOrder[i]); }
            catch (Exception ignored) {}
        }

        if (preferredGuild == null) preferredGuild = event.getJDA().getGuilds().get(0);

        LoggerFactory.getLogger(this.getClass()).info("* Bot is ready!");
        LoggerFactory.getLogger(this.getClass()).info("* Logged in as " + event.getJDA().getSelfUser().getAsTag() + "!");

        LoggerFactory.getLogger(this.getClass()).info("    * Preferred Guild: " + preferredGuild.getName());
        LoggerFactory.getLogger(this.getClass()).info("    * Guilds: " + Misc.getGuildList(event.getJDA().getGuilds(), true, true));

        //StarboardListener.updateAllSBM(event);

        for (var guild : event.getJDA().getGuilds()) {
            Bot.slashCommands.registerSlashCommands(guild);
        }
    }
}
