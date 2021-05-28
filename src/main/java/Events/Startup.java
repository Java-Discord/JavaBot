package Events;

import Commands.Other.Version;
import Other.Misc;
import Properties.ConfigString;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class Startup extends ListenerAdapter {

    public static String iae = "https://cdn.discordapp.com/attachments/838019016395063328/838019031628906496/IllegalArgumentException.png";
    public static String mfurle = "https://cdn.discordapp.com/attachments/838020992882049025/838021012871315486/MalformedURLException.png";

    public static MongoClient mongoClient;
    public static SelfUser bot;

    @Override
    public void onReady(ReadyEvent event){

        bot = event.getJDA().getSelfUser();

        try {

            ConfigString login = new ConfigString("mongologin", "default");
            MongoClientURI uri = new MongoClientURI(login.getValue());
            mongoClient = new MongoClient(uri);

            LoggerFactory.getLogger(Startup.class).info("* Successfully connected to Database!");

        } catch(Exception e) {
            LoggerFactory.getLogger(Startup.class).error("* Couldn't connect to Database... Shutting down...");
            System.exit(0);
        }

        try {
            File f = new File("textfiles/startup.txt");
            StringBuilder sb = new StringBuilder();
            Scanner fReader = new Scanner(f);

            while (fReader.hasNextLine()) {
                sb.append(fReader.nextLine() + "\n");
            }
            System.out.println("\n" + sb.toString().replace("{!version}", new Version().getVersion()));

        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(Startup.class).error("* textfiles/startup.txt not found");
        }

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LoggerFactory.getLogger(Startup.class).info("* Bot is ready!");
        LoggerFactory.getLogger(Startup.class).info("* Logged in as " + event.getJDA().getSelfUser().getAsTag() + "!");

        LoggerFactory.getLogger(Startup.class).info("    * Guilds: " + Misc.getGuildList(event.getJDA().getGuilds(), true, true));

        //StarboardListener.updateAllSBM(event);



    }
}
