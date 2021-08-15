package com.javadiscord.javabot.commands.other.qotw;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import javax.imageio.ImageIO;
import javax.print.attribute.standard.MediaSize;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Indexes.descending;
import static com.mongodb.client.model.Projections.excludeId;

public class Leaderboard implements SlashCommandHandler {

    private final Color BACKGROUND_COLOR = Color.decode("#011E2F");

    private final int LB_WIDTH = 3000;
    private final int CARD_HEIGHT = 350;
    private final int EMPTY_SPACE = 700;

    private final float NAME_SIZE = 65;
    private final float PLACEMENT_SIZE = 72;

    @Override
    public ReplyAction handle (SlashCommandEvent event) {

        OptionMapping option = event.getOption("amount");
        long l = option == null ? 10 : option.getAsLong();

        if (l > 50 || l < 2) return Responses.error(event, "```Please choose an amount between 2-30```");

        Bot.asyncPool.submit(() -> {
            event.getChannel().sendFile(new ByteArrayInputStream(generateLB(event, l).toByteArray()), "leaderboard" + ".png").queue();
        });

     return event.deferReply();
    }

    public int getQOTWRank(Guild guild, String userid) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        ArrayList<String> users = new ArrayList<>();
        MongoCursor<Document> doc = collection.find().projection(excludeId()).sort(descending("qotwpoints")).iterator();

        while (doc.hasNext()) {

            JsonObject Root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
            String discordID = Root.get("discord_id").getAsString();
            if (guild.getMemberById(discordID) == null) continue;

            users.add(discordID);
        }

        return (users.indexOf(userid)) + 1;
    }

    ArrayList<Member> getTopUsers (Guild guild, int num) {

        ArrayList<Member> topUsers = new ArrayList<>();

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        MongoCursor<Document> doc = collection.find().projection(excludeId()).sort(descending("qotwpoints")).iterator();

        int placement = 1;
        while (doc.hasNext() && placement <= num) {

            JsonObject Root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
            String discordID = Root.get("discord_id").getAsString();
            if (guild.getMemberById(discordID) == null) continue;

            try { topUsers.add(guild.getMemberById(discordID));
            } catch (Exception e) { e.printStackTrace(); }

            placement++;
        }

        return topUsers;
    }

    BufferedImage getAvatar (String avatarURL)  {

        BufferedImage img = null;
        try { img = ImageIO.read(new URL(avatarURL));
        } catch (Exception e) { e.printStackTrace(); }

        return img;
    }

    BufferedImage getImage (String resourcePath) {

        BufferedImage img = null;
        try { img = ImageIO.read(Objects.requireNonNull(Leaderboard.class.getClassLoader().getResourceAsStream(resourcePath)));
        } catch (IOException e) { e.printStackTrace();}

        return img;
    }

    Font getFont (float size) {

        Font font;

        try {
            font = Font.createFont(Font.TRUETYPE_FONT, Leaderboard.class.getClassLoader().getResourceAsStream("fonts/Uni-Sans-Heavy.ttf")).deriveFont(size);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);

        } catch (IOException | FontFormatException e) {
            font = new Font("Arial", Font.PLAIN, (int) size);
        }

        return font;
    }

    void drawUserCard (Graphics2D g2d, Member member, int yOffset, boolean drawLeft, boolean topten) {

        // LEFT
        int xOffset = 200;

        // RIGHT
        if (!drawLeft) xOffset = 1588;

        // CENTER
        if (topten) xOffset = 894;

        g2d.drawImage(getAvatar(member.getUser().getEffectiveAvatarUrl() + "?size=4096"), xOffset + 185, yOffset + 43, 200, 200, null);

        if (topten) g2d.drawImage(getImage("images/leaderboard/LBSelfCard.png"), xOffset, yOffset, null);
        else g2d.drawImage(getImage("images/leaderboard/LBCard.png"), xOffset, yOffset, null);

        g2d.setColor(Color.WHITE);
        g2d.setFont(getFont(NAME_SIZE));

        int stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getName());

        while (stringWidth > 750) {

            Font currentFont = g2d.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() - 1F);
            g2d.setFont(newFont);
            stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getName());
        }

        g2d.drawString(member.getUser().getName(), xOffset + 430, yOffset + 130);

        g2d.setColor(Color.decode("#414A52"));
        g2d.setFont(getFont(PLACEMENT_SIZE));

        String text;
        int points = new Database().getMemberInt(member, "qotwpoints");

        if (points == 1) text = points + " point";
        else text = points + " points";

        String placement = "#" + getQOTWRank(member.getGuild(), member.getId());
        g2d.drawString(text, xOffset + 430, yOffset + 210);

        int stringLength = (int) g2d.getFontMetrics().getStringBounds(placement, g2d).getWidth();
        int start = 185 / 2 - stringLength / 2;

        g2d.drawString(placement, xOffset + start, yOffset + 165);
    }

     ByteArrayOutputStream generateLB (SlashCommandEvent event, long num) {

        int LB_HEIGHT = (getTopUsers(event.getGuild(), (int) num).size() / 2) * CARD_HEIGHT + EMPTY_SPACE + 20;
         boolean topTen = false;

         if (getTopUsers(event.getGuild(), (int) num).contains(event.getMember())) topTen = true;

         if (!topTen) LB_HEIGHT += CARD_HEIGHT;
         BufferedImage bufferedImage = new BufferedImage(LB_WIDTH, LB_HEIGHT, BufferedImage.TYPE_INT_RGB);

         Graphics2D g2d = bufferedImage.createGraphics();

         g2d.setRenderingHint(
                 RenderingHints.KEY_TEXT_ANTIALIASING,
                 RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

         g2d.setRenderingHint(
                 RenderingHints.KEY_FRACTIONALMETRICS,
                 RenderingHints.VALUE_FRACTIONALMETRICS_ON);

         g2d.setPaint(BACKGROUND_COLOR);
         g2d.fillRect(0, 0, LB_WIDTH, LB_HEIGHT);

         BufferedImage logo = getImage("images/leaderboard/Logo.png");

         g2d.drawImage(logo, LB_WIDTH / 2 - logo.getWidth() / 2, 110, null);

         int nameY = EMPTY_SPACE;
         boolean drawLeft = true;

         event.getHook().sendMessage("Fetching user data... (" + getTopUsers(event.getGuild(), (int) num).size() + ") " + Constants.LOADING).queue();

         for (var member : getTopUsers(event.getGuild(), (int) num)) {

             if (drawLeft) drawUserCard(g2d, member, nameY, true, false);
             else drawUserCard(g2d, member, nameY, false, false);

             drawLeft = !drawLeft;
             if (drawLeft) nameY = nameY + CARD_HEIGHT;
         }

         if (!topTen) drawUserCard(g2d, event.getMember(), nameY, true, true);

         g2d.dispose();

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         try { ImageIO.write(bufferedImage, "png", outputStream);
         } catch (IOException e) { e.printStackTrace(); }

         event.getHook().editOriginal("Done!").queue();
         return outputStream;
     }
}