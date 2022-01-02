package net.javadiscord.javabot.systems.staff;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

// TODO: Refactor embed editing interface completely.
@Deprecated(forRemoval = true)
public class EditEmbedCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        return switch (event.getSubcommandName()) {
            case "edit" -> editEmbed(event);
            case "from-message" -> editEmbedFromLink(event);
            default -> Responses.warning(event, "Unknown subcommand.");
        };
    }

    private ReplyAction editEmbedFromLink(SlashCommandEvent event) {
        String emLink = event.getOption("embed-link").getAsString();
        String msgLink = event.getOption("message-link").getAsString();

        String[] emValue = emLink.split("/");
        String[] msgValue = msgLink.split("/");

        Message emMessage, msgMessage;
        try {
            TextChannel emChannel = event.getGuild().getTextChannelById(emValue[5]);
            emMessage = emChannel.retrieveMessageById(emValue[6]).complete();
        } catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }

        try {
            TextChannel msgChannel = event.getGuild().getTextChannelById(msgValue[5]);
            msgMessage = msgChannel.retrieveMessageById(msgValue[6]).complete();
        } catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }

        OptionMapping embedOption = event.getOption("title");
        String title = embedOption == null ? emMessage.getEmbeds().get(0).getTitle() : embedOption.getAsString();

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(emMessage.getEmbeds().get(0).getColor())
                .setTitle(title)
                .setDescription(msgMessage.getContentRaw());

        emMessage.editMessageEmbeds(eb.build()).queue();
        return event.reply("Done!").setEphemeral(true);
    }

    private ReplyAction editEmbed(SlashCommandEvent event) {

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
        String title = embedOption == null ? message.getEmbeds().get(0).getTitle() : embedOption.getAsString();

        String description = event.getOption("description").getAsString();

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(message.getEmbeds().get(0).getColor())
                .setTitle(title)
                .setDescription(description);

        message.editMessageEmbeds(eb.build()).queue();
        return event.reply("Done!").setEphemeral(true);
    }
}