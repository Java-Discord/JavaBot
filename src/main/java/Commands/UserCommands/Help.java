package Commands.UserCommands;

import Other.Constants;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.button.Button;

public class Help extends Command {

    public static void exCommand (CommandEvent event) {

        EmbedBuilder eb = new EmbedBuilder()
                .setDescription("Visit **[this page](" + Constants.HELP_LINK + ")** for a full list of Commands")
                .setColor(Constants.GRAY);

        event.reply(eb.build());

    }

    public static void exCommand (SlashCommandEvent event) {

        EmbedBuilder eb = new EmbedBuilder()
                .setDescription("Visit **[this page](" + Constants.HELP_LINK + ")** for a full list of Commands")
                .setColor(Constants.GRAY);

        event.replyEmbeds(eb.build()).queue();
    }

    public Help () {
        this.name = "help";
        this.category = new Category("USER COMMANDS");
        this.help = "this message";
    }

    @Override
    protected void execute(CommandEvent event) {

        exCommand(event);
    }
}