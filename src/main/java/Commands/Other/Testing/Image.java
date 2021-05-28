package Commands.Other.Testing;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class Image extends Command {

    public Image () {
        this.name = "img";
        this.aliases = new String[]{"image"};
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.arguments = "<Link>";
        this.help = "takes a link and sends it in the current chat";
    }

    protected void execute(CommandEvent event) {

        String[] args = event.getArgs().split("\\s+");
        event.reply(args[0]);
        event.getMessage().delete().complete();
    }
}