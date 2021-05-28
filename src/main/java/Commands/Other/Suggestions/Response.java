package Commands.Other.Suggestions;

import Other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Arrays;

public class Response extends Command {

    public Response () {
        this.name = "response";
        this.category = new Category("OTHER");
        this.arguments = "<ID> <Text>";
        this.help = "adds a response to the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                String[] MessageArg = Arrays.copyOfRange(args, 1, args.length);
                StringBuilder builder = new StringBuilder();
                for (String value : MessageArg) {
                    builder.append(value + " ");
                }
                String Text = builder.substring(0, builder.toString().length() - 1);

                Message SuggestionMessage = event.getChannel().retrieveMessageById(args[0]).complete();
                MessageEmbed SuggestionMessageEmbed = event.getChannel().retrieveMessageById(args[0]).complete().getEmbeds().get(0);

                String AuthorName = SuggestionMessageEmbed.getAuthor().getName();
                String AuthorIcon = SuggestionMessageEmbed.getAuthor().getIconUrl();
                String Description = SuggestionMessageEmbed.getDescription();
                Color Color = SuggestionMessageEmbed.getColor();
                OffsetDateTime Timestamp = SuggestionMessageEmbed.getTimestamp();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Color)
                        .setAuthor(AuthorName, null, AuthorIcon)
                        .setDescription(Description)
                        .addField("→ Response from " + event.getAuthor().getAsTag(), Text, false)
                        .setTimestamp(Timestamp);

                SuggestionMessage.editMessage(eb.build()).queue();
                event.getMessage().delete().queue();

            } catch (Exception e) {
                event.reply(Embeds.syntaxError("response MessageID Text", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
        }
    }