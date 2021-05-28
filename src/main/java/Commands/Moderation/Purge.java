package Commands.Moderation;

import Other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;

import java.awt.*;
import java.util.List;

public class Purge extends Command {

    public Purge () { this.name = "purge"; }

    protected void execute(CommandEvent event) {
            if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

                String[] args = event.getArgs().split("\\s+");

                try {

                    if (args[0].equalsIgnoreCase("all")) {
                        event.getTextChannel().createCopy().queue();
                        event.getTextChannel().delete().queue();

                        return;
                    }

                    int amount = Integer.parseInt(args[0]);
                    MessageHistory history = new MessageHistory(event.getChannel());
                    List<Message> messages = history.retrievePast(amount + 1).complete();

                    //for (int i = messages.size() - 1; i > 0; i--) messages.get(i).delete().queue();
                    event.getTextChannel().deleteMessages(messages).complete();

                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(new Color(0x2F3136))
                            .setTitle("Successfully deleted **" + amount + " messages** :broom:");
                    event.reply(eb.build());
                    event.getMessage().delete().queue();

                } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                    event.reply(Embeds.purgeError(event));
                }
                } else {
                    event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
        }
    }