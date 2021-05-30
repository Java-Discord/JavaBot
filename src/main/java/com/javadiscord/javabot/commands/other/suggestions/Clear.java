package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;

import java.awt.*;
import java.time.OffsetDateTime;

public class Clear extends Command {

    public Clear () {
        this.name = "clear";
        this.category = new Category("OTHER");
        this.arguments = "<ID>";
        this.help = "clears the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                Emote Upvote = event.getGuild().getEmotesByName("up_vote", false).get(0);
                Emote Downvote = event.getGuild().getEmotesByName("down_vote", false).get(0);

                Message SuggestionMessage = event.getChannel().retrieveMessageById(args[0]).complete();
                SuggestionMessage.clearReactions().queue();

                String AuthorName = SuggestionMessage.getEmbeds().get(0).getAuthor().getName();
                String AuthorIcon = SuggestionMessage.getEmbeds().get(0).getAuthor().getIconUrl();
                String Description = SuggestionMessage.getEmbeds().get(0).getDescription();
                OffsetDateTime Timestamp = SuggestionMessage.getEmbeds().get(0).getTimestamp();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(new Color(0x2F3136))
                        .setAuthor(AuthorName, null, AuthorIcon)
                        .setDescription(Description)
                        .setTimestamp(Timestamp);

                SuggestionMessage.editMessage(eb.build()).queue(message1 -> {
                    message1.addReaction(Upvote).queue();
                    message1.addReaction(Downvote).queue();
                });

                event.getMessage().delete().queue();

            } catch (Exception e){
                event.reply(Embeds.syntaxError("clear MessageID", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
    }
}
