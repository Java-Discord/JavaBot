package com.javadiscord.javabot.commands.other.testing;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.javadiscord.javabot.events.GuildJoin;

public class AddConfigFile extends Command {

    public AddConfigFile() {
        this.name = "addguild";
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.arguments = "<ID>";
        this.help = "adds the given guild to the database";
    }

    protected void execute(CommandEvent event) {

        String[] args = event.getArgs().split("\\s+");

        GuildJoin.addGuildToDB(args[0], args[1]);
    }
}