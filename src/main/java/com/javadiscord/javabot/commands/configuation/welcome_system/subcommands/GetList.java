package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.events.UserJoin;
import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.io.ByteArrayInputStream;

public class GetList implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Bot.asyncPool.submit(() -> this.send(event));
        return event.deferReply();
    }

    private void send(SlashCommandEvent event) {
        var config = Bot.config.get(event.getGuild()).getWelcome();
        String status = config.isEnabled() ? "enabled" : "disabled";

        var eb = new EmbedBuilder()
                .setTitle("Welcome System Configuration")
                .setColor(Constants.GRAY)

                .addField("Image", "Width, Height: `" + config.getImageConfig().getWidth() +
                        "`, `" + config.getImageConfig().getHeight() +
                        "`\n[Overlay](" + config.getImageConfig().getOverlayImageUrl() +
                        "), [Background](" + config.getImageConfig().getBackgroundImageUrl() + ")", false)

                .addField("Color", "Primary Color: `#" + Integer.toHexString(config.getImageConfig().getPrimaryColor()) +
                        "`\nSecondary Color: `#" + Integer.toHexString(config.getImageConfig().getSecondaryColor()) + "`", true)

                .addField("Avatar Image", "Width, Height: `" + config.getImageConfig().getAvatarConfig().getWidth() +
                        "`,`" + config.getImageConfig().getAvatarConfig().getHeight() +
                        "`\nX, Y: `" + config.getImageConfig().getAvatarConfig().getX() +
                        "`, `" + config.getImageConfig().getAvatarConfig().getY() + "`", true)

                .addField("Messages", "Join: `" + config.getJoinMessageTemplate() +
                        "`\nLeave: `" + config.getLeaveMessageTemplate() + "`", false)

                .addField("Channel", config.getChannel().getAsMention(), true)
                .addField("Status", "``" + status + "``", true);

        try {
            event.getHook().sendMessageEmbeds(eb.build())
                    .addFile(
                            new ByteArrayInputStream(
                            new UserJoin().generateImage(event.getGuild(), event.getMember())),
                            event.getMember().getId() + ".png")
                    .queue();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
