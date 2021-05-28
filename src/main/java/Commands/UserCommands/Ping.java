package Commands.UserCommands;

import Events.Startup;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;

public class Ping extends Command {

    public static void exCommand (CommandEvent event) {

        long gatewayPing = event.getJDA().getGatewayPing();
        String botImage = Startup.bot.getAvatarUrl();

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(gatewayPing + "ms", null, botImage)
                .setColor(new Color(0x2F3136));
        event.reply(eb.build());
    }

    public static void exCommand (SlashCommandEvent event) {

        long gatewayPing = event.getJDA().getGatewayPing();
        String botImage = Startup.bot.getAvatarUrl();

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(gatewayPing + "ms", null, botImage)
                .setColor(new Color(0x2F3136));

        event.replyEmbeds(eb.build()).queue();
    }

    public Ping() {

        this.name = "ping";
        this.aliases = new String[]{"pong"};
        this.category = new Category("USER COMMANDS");
        this.help = "Checks Java's gateway ping";
    }

    protected void execute(CommandEvent event) {

       exCommand(event);
    }
}