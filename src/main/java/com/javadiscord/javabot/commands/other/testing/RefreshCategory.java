package com.javadiscord.javabot.commands.other.testing;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.javadiscord.javabot.other.StatsCategory;

public class RefreshCategory extends Command {

    public RefreshCategory () {
        this.name = "refreshcategory";
        this.aliases = new String[]{"rcat"};
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.help = "refreshes the stats-category";
    }

    protected void execute(CommandEvent event) {

        StatsCategory.update(event);
        event.reactSuccess();
    }
}

