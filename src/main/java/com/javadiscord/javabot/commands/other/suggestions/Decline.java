package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.OffsetDateTime;

public class Decline extends Command {

    public Decline () {
        this.name = "decline";
        this.category = new Category("OTHER");
        this.arguments = "<ID>";
        this.help = "declines the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                Emote Abort = event.getGuild().getEmotesByName("abort", false).get(0);

                Message SuggestionMessage = event.getChannel().retrieveMessageById(args[0]).complete();
                MessageEmbed SuggestionMessageEmbed = event.getChannel().retrieveMessageById(args[0]).complete().getEmbeds().get(0);
                SuggestionMessage.clearReactions().queue();

                String AuthorName = SuggestionMessage.getEmbeds().get(0).getAuthor().getName();
                String AuthorIcon = SuggestionMessage.getEmbeds().get(0).getAuthor().getIconUrl();
                String Description = SuggestionMessage.getEmbeds().get(0).getDescription();
                OffsetDateTime Timestamp = SuggestionMessage.getEmbeds().get(0).getTimestamp();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(new Color(0xe74c3c))
                        .setAuthor(AuthorName, null, AuthorIcon);

                try {
                    String ResponseFieldName = SuggestionMessageEmbed.getFields().get(0).getName();
                    String ResponseFieldValue = SuggestionMessageEmbed.getFields().get(0).getValue();

                    eb.addField(ResponseFieldName, ResponseFieldValue, false);

                } catch (IndexOutOfBoundsException e) {}

                eb.setDescription(Description)
                        .setTimestamp(Timestamp)
                        .setFooter("Declined by " + event.getAuthor().getAsTag());

                SuggestionMessage.editMessage(eb.build()).queue(message1 -> message1.addReaction(Abort).queue());

                event.getMessage().delete().queue();

            } catch (Exception e) {
                event.reply(Embeds.syntaxError("decline MessageID", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
    }
}