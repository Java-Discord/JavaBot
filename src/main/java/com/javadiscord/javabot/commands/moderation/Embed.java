package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;

public class Embed implements SlashCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        switch (event.getSubcommandName()) {
            case "create": createEmbed(event); break;
            case "from-message": createEmbedFromLink(event); break;
        }
    }

    void createEmbedFromLink(SlashCommandEvent event) {
        MessageChannel channel = event.getOption("channel").getAsMessageChannel();
        String messageID = event.getOption("message-id").getAsString();

        try {
        Message message = channel.retrieveMessageById(messageID).complete();

        var e = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setDescription(message.getContentRaw())
                .build();

        event.getChannel().sendMessageEmbeds(e).queue();
        event.reply("Done!").setEphemeral(true).queue();

        } catch (Exception e) { event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
    }

    void createEmbed(SlashCommandEvent event) {

            OptionMapping embedOption;
            embedOption = event.getOption("title");
            String title = embedOption == null ? null : embedOption.getAsString();

            embedOption = event.getOption("description");
            String description = embedOption == null ? null : embedOption.getAsString();

            embedOption = event.getOption("author-name");
            String authorname = embedOption == null ? null : embedOption.getAsString();

            embedOption = event.getOption("author-url");
            String url = embedOption == null ? null : embedOption.getAsString();

            embedOption = event.getOption("author-iconurl");
            String iconurl = embedOption == null ? null : embedOption.getAsString();

            embedOption = event.getOption("thumbnail-url");
            String thumb = embedOption == null ? null : embedOption.getAsString();

            embedOption = event.getOption("image-url");
            String img = embedOption == null ? null : embedOption.getAsString();

            embedOption = event.getOption("color");
            String color = embedOption == null ? null : embedOption.getAsString();
            try {
                var eb = new EmbedBuilder();

                eb.setTitle(title);
                eb.setDescription(description);
                eb.setAuthor(authorname, url, iconurl);
                eb.setImage(img);
                eb.setThumbnail(thumb);

                if (!(color == null)) {
                    try {
                        eb.setColor(Color.decode(color));
                    } catch (Exception e) {
                        eb.setColor(Constants.GRAY);
                    }
                }

                event.replyEmbeds(eb.build()).queue();

            } catch (Exception e) { event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
    }
}