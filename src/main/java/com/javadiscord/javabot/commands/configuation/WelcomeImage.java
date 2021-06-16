package com.javadiscord.javabot.commands.configuation;

import com.google.gson.JsonObject;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class WelcomeImage {

    public static void setImageWidth(SlashCommandEvent event, int width) {

        Database.queryWelcomeImageInt(event.getGuild().getId(), "imgW", width);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Width", "Welcome Image Width succesfully changed to ", null, String.valueOf(width), true)).queue();
    }

    public static void setImageHeight(SlashCommandEvent event, int height) {

        Database.queryWelcomeImageInt(event.getGuild().getId(), "imgH", height);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Height", "Welcome Image Height succesfully changed to ", null, String.valueOf(height), true)).queue();
    }

    public static void setOverlayURL(SlashCommandEvent event, String url) {

        if (Misc.isImage(url)) {
            Database.queryWelcomeImageString(event.getGuild().getId(), "overlayURL", url);
            event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Overlay", "Welcome Image Overlay succesfully changed to ", Misc.checkImage(url), url, true)).queue();
        } else {
            event.replyEmbeds(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event)).queue();
        }
    }

    public static void setBackgroundURL(SlashCommandEvent event, String url) {

        if (Misc.isImage(url)) {
            Database.queryWelcomeImageString(event.getGuild().getId(), "bgURL", url);
            event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Background", "Welcome Image Background succesfully changed to ", Misc.checkImage(url), url, true)).queue();
        } else {
            event.replyEmbeds(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event)).queue();
        }
    }

    public static void setPrimaryColor(SlashCommandEvent event, String color) {

        long l = Long.parseLong(color, 16);
        int i = (int) l;

        Database.queryWelcomeImageInt(event.getGuild().getId(), "primCol", i);
        event.replyEmbeds(Embeds.configEmbed(event, "Primary Welcome Image Color", "Primary Welcome Image Color succesfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true)).queue();
    }

    public static void setSecondaryColor(SlashCommandEvent event, String color) {

        long l = Long.parseLong(color, 16);
        int i = (int) l;

        Database.queryWelcomeImageInt(event.getGuild().getId(), "secCol", i);
        event.replyEmbeds(Embeds.configEmbed(event, "Secondary Welcome Image Color", "Secondary Welcome Image Color succesfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true)).queue();
    }

    public static void setAvatarHeight(SlashCommandEvent event, int height) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avH", height);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image Height", "Avatar Image Height succesfully changed to ", null, String.valueOf(height), true)).queue();
    }

    public static void setAvatarWidth(SlashCommandEvent event, int width) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avW", width);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image Width", "Avatar Image Width succesfully changed to ", null, String.valueOf(width), true)).queue();
    }

    public static void setAvatarX(SlashCommandEvent event, int x) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avX", x);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image (X-Pos)", "Avatar Image ``(X-Position)`` succesfully changed to ", null, String.valueOf(x), true)).queue();
    }

    public static void setAvatarY(SlashCommandEvent event, int y) {

        Database.queryAvatarImageInt(event.getGuild().getId(), "avY", y);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image (Y-Pos)", "Avatar Image ``(Y-Position)`` succesfully changed to ", null, String.valueOf(y), true)).queue();
    }

    public static void getList(SlashCommandEvent event) {

        String guildID = event.getGuild().getId();
        JsonObject welcomeImage = Database.welcomeImage(guildID);
        JsonObject avatarImage = Database.avatarImage(guildID);
        String overlayURL = Database.welcomeImage(event.getGuild().getId()).get("overlayURL").getAsString();

        var eb = new EmbedBuilder()
                .setTitle("Welcome Image Configuration")
                .setColor(Constants.GRAY)

                .setImage(Misc.checkImage(overlayURL))

                .addField("Image", "Width, Height: ``" + welcomeImage.get("imgW").getAsString() +
                        "``, ``" + welcomeImage.get("imgH").getAsString() +
                        "``\n\n[Overlay](" + overlayURL +
                        "), [Background](" + welcomeImage.get("bgURL").getAsString() + ")", false)

                .addField("Color", "Primary Color: ``#" + Integer.toHexString(welcomeImage.get("primCol").getAsInt()) +
                        "``\nSecondary Color: ``#" + Integer.toHexString(welcomeImage.get("secCol").getAsInt()) + "``", true)

                .addField("Avatar Image", "Width, Height: ``" + avatarImage.get("avW").getAsString() +
                        "``,``" + avatarImage.get("avH").getAsString() +
                        "``\nX, Y: ``" + avatarImage.get("avX").getAsString() +
                        "``, ``" + avatarImage.get("avY").getAsString() + "``", true)
                .build();

        event.replyEmbeds(eb).queue();

    }
}

