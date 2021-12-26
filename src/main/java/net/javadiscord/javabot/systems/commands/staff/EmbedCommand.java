package net.javadiscord.javabot.systems.commands.staff;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.awt.*;
import java.util.function.UnaryOperator;

// TODO: Refactor embed interface completely.
@Deprecated(forRemoval = true)
public class EmbedCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        return switch (event.getSubcommandName()) {
            case "create" -> createEmbed(event);
            case "from-message" -> createEmbedFromLink(event);
            default -> Responses.warning(event, "Unknown subcommand.");
        };
    }

    private ReplyAction createEmbedFromLink(SlashCommandEvent event) {
        String link = event.getOption("link").getAsString();
        String[] value = link.split("/");

        Message message;

        try {
            TextChannel channel = event.getGuild().getTextChannelById(value[5]);
            message = channel.retrieveMessageById(value[6]).complete();
        } catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }

        OptionMapping embedOption = event.getOption("title");
        String title = embedOption == null ? null : embedOption.getAsString();

        var eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setTitle(title)
                .setDescription(message.getContentRaw())
                .build();

        event.getChannel().sendMessageEmbeds(eb).queue();
        return event.reply("Done!").setEphemeral(true);
    }

    private ReplyAction createEmbed(SlashCommandEvent event) {
        UnaryOperator<String> getOpt = s -> {
            var mapping = event.getOption(s);
            return mapping == null ? null : mapping.getAsString();
        };
        String title = getOpt.apply("title");
        String description = getOpt.apply("description");
        String authorname = getOpt.apply("author-name");
        String url = getOpt.apply("author-url");
        String iconurl = getOpt.apply("author-iconurl");
        String thumb = getOpt.apply("thumbnail-url");
        String img = getOpt.apply("image-url");
        String color = getOpt.apply("color");
        try {
            var eb = new EmbedBuilder();
            eb.setTitle(title);
            eb.setDescription(description);
            eb.setAuthor(authorname, url, iconurl);
            eb.setImage(img);
            eb.setThumbnail(thumb);
            eb.setColor(Color.decode(color));

            event.getChannel().sendMessageEmbeds(eb.build()).queue();
            return event.reply("Done!").setEphemeral(true);

        } catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }
    }
}