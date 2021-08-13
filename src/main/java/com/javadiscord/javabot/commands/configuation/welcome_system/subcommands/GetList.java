package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.events.UserJoin;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
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
        String status;
        if (Database.getConfigBoolean(event.getGuild(), "welcome_system.welcome_status")) status = "enabled";
        else status = "disabled";

        var eb = new EmbedBuilder()
                .setTitle("Welcome System Configuration")
                .setColor(Constants.GRAY)

                .addField("Image", "Width, Height: `" + Database.getConfigString(event.getGuild(), "welcome_system.image.imgW") +
                        "`, `" + Database.getConfigString(event.getGuild(), "welcome_system.image.imgH") +
                        "`\n[Overlay](" + Database.getConfigString(event.getGuild(), "welcome_system.image.overlayURL") +
                        "), [Background](" + Database.getConfigString(event.getGuild(), "welcome_system.image.bgURL") + ")", false)

                .addField("Color", "Primary Color: `#" + Integer.toHexString(Database.getConfigInt(event.getGuild(), "welcome_system.image.primCol")) +
                        "`\nSecondary Color: `#" + Integer.toHexString(Database.getConfigInt(event.getGuild(), "welcome_system.image.secCol")) + "`", true)

                .addField("Avatar Image", "Width, Height: `" + Database.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avW") +
                        "`,`" + Database.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avH") +
                        "`\nX, Y: `" + Database.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avX") +
                        "`, `" + Database.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avY") + "`", true)

                .addField("Messages", "Join: `" + Database.getConfigString(event.getGuild(), "welcome_system.join_msg") +
                        "`\nLeave: `" + Database.getConfigString(event.getGuild(), "welcome_system.leave_msg") + "`", false)

                .addField("Channel", Database.getConfigChannelAsMention(event.getGuild(), "welcome_system.welcome_cid"), true)
                .addField("Status", "``" + status + "``", true);

        try {
            event.getHook().editOriginalEmbeds(eb.build()).addFile(new ByteArrayInputStream(new UserJoin().generateImage(event, false, false)), event.getMember().getId() + ".png").queue();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
