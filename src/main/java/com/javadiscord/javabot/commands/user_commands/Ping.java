package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class Ping implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        String botImage = event.getJDA().getSelfUser().getAvatarUrl();

        var e = new EmbedBuilder()
            .setAuthor(gatewayPing + "ms", null, botImage)
            .setColor(Constants.GRAY)
            .build();

        return event.replyEmbeds(e);
    }
}