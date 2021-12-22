package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

public class BanCommand implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var userOption = event.getOption("user");
        if (userOption == null) return Responses.error(event, "User Option may not be null");
        var user = userOption.getAsUser();
        var reasonOption = event.getOption("reason");
        if (reasonOption == null) return Responses.error(event, "Reason may not be null");
        String reason = reasonOption.getAsString();

        var channel = event.getTextChannel();
        if (channel.getType() != ChannelType.TEXT) return Responses.error(event, "This command can only be performed in a server text channel.");

        var quietOption = event.getOption("quiet");
        boolean quiet = quietOption != null && quietOption.getAsBoolean();

        var moderationService = new ModerationService(event.getInteraction());
        moderationService.ban(user, reason, event.getUser(), channel, quiet);
        return Responses.success(event, "User Banned", String.format("User %s has been banned.", user.getAsTag()));
    }
}