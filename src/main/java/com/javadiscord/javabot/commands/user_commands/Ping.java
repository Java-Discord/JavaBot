package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class Ping {

    public static void execute(SlashCommandEvent event) {

        long gatewayPing = event.getJDA().getGatewayPing();
        String botImage = event.getJDA().getSelfUser().getAvatarUrl();

        var e = new EmbedBuilder()
            .setAuthor(gatewayPing + "ms", null, botImage)
            .setColor(Constants.GRAY)
            .build();

        event.replyEmbeds(e).queue();
    }
}