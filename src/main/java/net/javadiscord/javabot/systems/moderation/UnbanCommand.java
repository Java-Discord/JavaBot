package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.util.Misc;

import java.time.Instant;

public class UnbanCommand implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        String id = event.getOption("id").getAsString();
        User author = event.getUser();

        var eb = unbanEmbed(event.getGuild(), author, id);

        try {
            event.getGuild().unban(id).complete();

            Misc.sendToLog(event.getGuild(), eb);
            return event.replyEmbeds(eb);
        } catch (ErrorResponseException e) {
            return Responses.error(event, "```Couldn't find user with id " + id + "```");
        }
    }

    public MessageEmbed unbanEmbed(Guild guild, User author, String id) {
        return new EmbedBuilder()
                .setAuthor("Unban")
                .setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
                .addField("ID", "```" + id + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .setFooter("ID: " + id)
                .setTimestamp(Instant.now())
                .build();
    }

    /**
     * Handles an interaction, that should unban a user from the current guild.
     * @param event The ButtonClickEvent, that is triggered upon use.
     * @param id The Id of the user that should be unbanned.
     */
    public RestAction<?> handleUnbanInteraction(ButtonClickEvent event, String id) {
        try {
            // TODO: Fix this throwing an exception if the user isn't banned (anymore)
            event.getGuild().unban(id).queue();
            event.getHook().editOriginalComponents()
                    .setActionRows(
                            ActionRow.of(
                                    Button.danger("dummy-button", "Unbanned user with ID " + id).asDisabled())
                    ).queue();
            return event.getChannel().sendMessageEmbeds(unbanEmbed(event.getGuild(), event.getUser(), id));

        } catch (Exception e) {
            event.getHook().editOriginalComponents().setActionRows(ActionRow.of(
                    Button.secondary("dummy-button", "Couldn't find Member").asDisabled())
            ).queue();
            return Responses.error(event.getHook(), "Couldn't find member");
        }
    }
}


