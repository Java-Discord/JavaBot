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

        String id = event.getUser().getId();

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Command List")
                .setDescription("**" + event.getJDA().getSelfUser().getName() + Constants.HELP_MAIN)
                .setThumbnail(event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                .setColor(Constants.GRAY);

        event.replyEmbeds(eb.build()).addActionRow(
                Button.secondary(id + ":help-home", "\uD83C\uDFE0"),
                Button.secondary(id + ":help-user", "User Commands"),
                Button.secondary(id + ":help-mod", "Moderation"),
                Button.secondary(id + ":help-other", "Other")
        ).queue();

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