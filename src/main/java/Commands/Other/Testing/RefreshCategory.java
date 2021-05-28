package Commands.Other.Testing;

import Other.StatsCategory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

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

