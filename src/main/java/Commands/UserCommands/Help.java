package Commands.UserCommands;

import Other.Constants;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.button.Button;

public class Help extends Command {

    public static void exCommand (CommandEvent event) {

        event.getMessage().addReaction(Constants.CHECK).complete();

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("Command List", null)
                .setDescription("Full Image: [Link](" + Constants.HELP_IMAGE + ")")
                .setImage(Constants.HELP_IMAGE)
                .setColor(Constants.GRAY);
        event.replyInDm(eb.build());

    }

    public static void exCommand (SlashCommandEvent event) {

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("Command List", null)
                .setDescription("Full Image: [Link](" + Constants.HELP_IMAGE + ")")
                .setImage(Constants.HELP_IMAGE)
                .setColor(Constants.GRAY);

        event.getUser().openPrivateChannel().complete().sendMessage(eb.build()).queue();
        event.reply("I've sent you a DM!").queue();
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