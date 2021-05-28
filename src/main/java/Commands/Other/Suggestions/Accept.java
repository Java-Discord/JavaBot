package Commands.Other.Suggestions;

import Other.Constants;
import Other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.OffsetDateTime;

public class Accept extends Command {

    public Accept () {
        this.name = "accept";
        this.category = new Category("OTHER");
        this.arguments = "<ID>";
        this.help = "accepts the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                Emote check = event.getGuild().getEmotesByName("check", false).get(0);

                Message SuggestionMessage = event.getChannel().retrieveMessageById(args[0]).complete();
                MessageEmbed SuggestionMessageEmbed = event.getChannel().retrieveMessageById(args[0]).complete().getEmbeds().get(0);
                SuggestionMessage.clearReactions().queue();

                String authorName = SuggestionMessage.getEmbeds().get(0).getAuthor().getName();
                String authorIcon = SuggestionMessage.getEmbeds().get(0).getAuthor().getIconUrl();
                String description = SuggestionMessage.getEmbeds().get(0).getDescription();
                OffsetDateTime timestamp = SuggestionMessage.getEmbeds().get(0).getTimestamp();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Constants.GREEN)
                        .setAuthor(authorName, null, authorIcon);

                try {
                    String responseFieldName = SuggestionMessageEmbed.getFields().get(0).getName();
                    String responseFieldValue = SuggestionMessageEmbed.getFields().get(0).getValue();

                    eb.addField(responseFieldName, responseFieldValue, false);

                } catch (IndexOutOfBoundsException e) {}

                eb.setDescription(description)
                        .setTimestamp(timestamp)
                        .setFooter("Accepted by " + event.getAuthor().getAsTag());

                SuggestionMessage.editMessage(eb.build()).queue(message1 -> message1.addReaction(check).queue());

                event.getMessage().delete().queue();

            } catch (Exception e) {
                event.reply(Embeds.syntaxError("accept MessageID", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
        }
    }