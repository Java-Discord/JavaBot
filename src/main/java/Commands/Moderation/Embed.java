package Commands.Moderation;

import Other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.awt.*;

public class Embed extends Command {

    public Embed () {
        this.name = "embed";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

                try {
                    String[] embedargs = event.getMessage().getContentRaw().split("~;");

                    String Title = embedargs[0].substring(7);
                    String Description = embedargs[1];

                    event.getMessage().delete().queue();
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(new Color(0x2F3136))
                            .setTitle(Title)
                            .setDescription(Description);

                    event.reply(eb.build());

                } catch (IndexOutOfBoundsException e) {
                    event.reply(Embeds.syntaxError("embed Title~;Description", event));
                }

                } else {
                    event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
                }
            }
        }