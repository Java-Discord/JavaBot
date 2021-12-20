package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.SlashCommandHandler;

public class PingCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        String botImage = event.getJDA().getSelfUser().getAvatarUrl();

        var e = new EmbedBuilder()
            .setAuthor(gatewayPing + "ms", null, botImage)
            .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
            .build();

        return event.replyEmbeds(e);
    }
}