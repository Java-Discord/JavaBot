package com.javadiscord.javabot.commands.other;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.javadiscord.javabot.other.Embeds;
import org.slf4j.LoggerFactory;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class Shutdown extends Command {

    public Shutdown () {
        this.name = "shutdown";
        this.aliases = new String[]{"shut"};
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.help = "Shuts down the bot";
    }

    protected void execute(CommandEvent event) {

        event.reply(Embeds.emptyEmbed("Shutdown", "```Shutting down...```", null, event));

        mongoClient.close();
        LoggerFactory.getLogger(Shutdown.class).info("Database disconnected...");
        LoggerFactory.getLogger(Shutdown.class).info("Shutting down...");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}

