package com.javadiscord.javabot.commands.configuation;

import com.google.gson.JsonObject;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import javax.xml.crypto.Data;

public class WelcomeSystem implements SlashCommandHandler {

    public static void setLeaveMessage(SlashCommandEvent event, String message) {

        Database.queryConfigString(event.getGuild().getId(), "leave_msg", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Leave Message", "Leave Message successfully changed to", null, message, true)).queue();
    }

    public static void setWelcomeMessage(SlashCommandEvent event, String message) {

        Database.queryConfigString(event.getGuild().getId(), "welcome_msg", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Message", "Welcome Message successfully changed to", null, message, true)).queue();
    }

    public static void setWelcomeChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfigString(event.getGuild().getId(), "welcome_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Channel", "Welcome Channel successfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setImageWidth(SlashCommandEvent event, int width) {

        Database.queryWelcomeImageInt(event.getGuild().getId(), "imgW", width);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Width", "Welcome Image Width successfully changed to ", null, String.valueOf(width), true)).queue();
    }

    public static void setImageHeight(SlashCommandEvent event, int height) {

        Database.queryWelcomeImageInt(event.getGuild().getId(), "imgH", height);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Height", "Welcome Image Height successfully changed to ", null, String.valueOf(height), true)).queue();
    }

    public static void setOverlayURL(SlashCommandEvent event, String url) {

        if (Misc.isImage(url)) {
            Database.queryWelcomeImageString(event.getGuild().getId(), "overlayURL", url);
            event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Overlay", "Welcome Image Overlay successfully changed to ", Misc.checkImage(url), url, true)).queue();
        } else {
            event.replyEmbeds(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event)).queue();
        }
    }

    public static void setBackgroundURL(SlashCommandEvent event, String url) {

        if (Misc.isImage(url)) {
            Database.queryWelcomeImageString(event.getGuild().getId(), "bgURL", url);
            event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Background", "Welcome Image Background successfully changed to ", Misc.checkImage(url), url, true)).queue();
        } else {
            event.replyEmbeds(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event)).queue();
        }
    }

    public static void setPrimaryColor(SlashCommandEvent event, String color) {

        long l = Long.parseLong(color, 16);
        int i = (int) l;

        Database.queryWelcomeImageInt(event.getGuild().getId(), "primCol", i);
        event.replyEmbeds(Embeds.configEmbed(event, "Primary Welcome Image Color", "Primary Welcome Image Color successfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true)).queue();
    }

    public static void setSecondaryColor(SlashCommandEvent event, String color) {

        long l = Long.parseLong(color, 16);
        int i = (int) l;

        Database.queryWelcomeImageInt(event.getGuild().getId(), "secCol", i);
        event.replyEmbeds(Embeds.configEmbed(event, "Secondary Welcome Image Color", "Secondary Welcome Image Color successfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true)).queue();
    }

    public static void setAvatarHeight(SlashCommandEvent event, int height) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avH", height);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image Height", "Avatar Image Height successfully changed to ", null, String.valueOf(height), true)).queue();
    }

    public static void setAvatarWidth(SlashCommandEvent event, int width) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avW", width);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image Width", "Avatar Image Width successfully changed to ", null, String.valueOf(width), true)).queue();
    }

    public static void setAvatarX(SlashCommandEvent event, int x) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avX", x);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image (X-Pos)", "Avatar Image ``(X-Position)`` successfully changed to ", null, String.valueOf(x), true)).queue();
    }

    public static void setAvatarY(SlashCommandEvent event, int y) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avY", y);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image (Y-Pos)", "Avatar Image ``(Y-Position)`` successfully changed to ", null, String.valueOf(y), true)).queue();
    }

    public static void setStatus(SlashCommandEvent event, boolean status) {

        Database.queryConfigBoolean(event.getGuild().getId(), "welcome_status", status);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome System Status changed", "Status successfully changed to ", null, String.valueOf(status), true)).queue();
    }

    public static void getList(SlashCommandEvent event) {

        String guildID = event.getGuild().getId();
        JsonObject welcomeImage = Database.welcomeImage(guildID);
        JsonObject avatarImage = Database.avatarImage(guildID);
        String overlayURL = Database.welcomeImage(event.getGuild().getId()).get("overlayURL").getAsString();

        String status;
        if (Database.getConfigBoolean(event, "welcome_status")) status = "enabled";
        else status = "disabled";

        var eb = new EmbedBuilder()
                .setTitle("Welcome System Configuration")
                .setColor(Constants.GRAY)

                .setImage(Misc.checkImage(overlayURL))

                .addField("Image", "Width, Height: ``" + welcomeImage.get("imgW").getAsString() +
                        "``, ``" + welcomeImage.get("imgH").getAsString() +
                        "``\n[Overlay](" + overlayURL +
                        "), [Background](" + welcomeImage.get("bgURL").getAsString() + ")", false)

                .addField("Color", "Primary Color: ``#" + Integer.toHexString(welcomeImage.get("primCol").getAsInt()) +
                        "``\nSecondary Color: ``#" + Integer.toHexString(welcomeImage.get("secCol").getAsInt()) + "``", true)

                .addField("Avatar Image", "Width, Height: ``" + avatarImage.get("avW").getAsString() +
                        "``,``" + avatarImage.get("avH").getAsString() +
                        "``\nX, Y: ``" + avatarImage.get("avX").getAsString() +
                        "``, ``" + avatarImage.get("avY").getAsString() + "``", true)

                .addField("Messages", "Welcome: ``" + Database.getConfigString(event, "welcome_msg") +
                        "``\nLeave: ``" + Database.getConfigString(event, "leave_msg") + "``", false)

                .addField("Channel", Database.configChannel(event, "welcome_cid").getAsMention(), true)
                .addField("Status", "``" + status + "``", true)
                .build();

        event.replyEmbeds(eb).queue();

    }

    @Override
    public void handle(SlashCommandEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            switch (event.getSubcommandName()) {
                case "list":
                    WelcomeSystem.getList(event);
                    break;

                case "status":
                    WelcomeSystem.setStatus(event, event.getOption("status").getAsBoolean());
                    break;

                case "leave-msg":
                    WelcomeSystem.setLeaveMessage(event, event.getOption("message").getAsString());
                    break;

                case "join-msg":
                    WelcomeSystem.setWelcomeMessage(event, event.getOption("message").getAsString());
                    break;

                case "channel":
                    WelcomeSystem.setWelcomeChannel(event, event.getOption("channel").getAsMessageChannel());
                    break;

                case "image-width":
                    WelcomeSystem.setImageWidth(event, (int) event.getOption("width").getAsLong());
                    break;

                case "image-height":
                    WelcomeSystem.setImageHeight(event, (int) event.getOption("height").getAsLong());
                    break;

                case "overlay-url":
                    WelcomeSystem.setOverlayURL(event, event.getOption("url").getAsString());
                    break;

                case "background-url":
                    WelcomeSystem.setBackgroundURL(event, event.getOption("url").getAsString());
                    break;

                case "primary-color":
                    WelcomeSystem.setPrimaryColor(event, event.getOption("color").getAsString());
                    break;

                case "secondary-color":
                    WelcomeSystem.setSecondaryColor(event, event.getOption("color").getAsString());
                    break;

                case "avatar-x":
                    WelcomeSystem.setAvatarX(event, (int) event.getOption("x").getAsLong());
                    break;

                case "avatar-y":
                    WelcomeSystem.setAvatarY(event, (int) event.getOption("y").getAsLong());
                    break;

                case "avatar-width":
                    WelcomeSystem.setAvatarWidth(event, (int) event.getOption("width").getAsLong());
                    break;

                case "avatar-height":
                    WelcomeSystem.setAvatarHeight(event, (int) event.getOption("height").getAsLong());
                    break;
            }
        } else {
            event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }
}

