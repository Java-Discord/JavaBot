package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Say implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            return event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }

        String text = event.getOption("text").getAsString();

        event.getChannel().sendMessage(text).queue();
        return event.reply("Done!").setEphemeral(true);
    }
}