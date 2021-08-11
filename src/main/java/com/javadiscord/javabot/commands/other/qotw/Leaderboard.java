package com.javadiscord.javabot.commands.other.qotw;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import javax.imageio.ImageIO;
import javax.print.attribute.standard.MediaSize;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Indexes.descending;
import static com.mongodb.client.model.Projections.excludeId;

public class Leaderboard implements SlashCommandHandler {

    private final int LB_WIDTH = 2000;
    private final int LB_HEIGHT = 3200;
    private final int LB_HEIGHT_TOP = 3000;

    private final float NAME_SIZE = 65;
    private final float POINTS_SIZE = 40;

    @Override
    public ReplyAction handle (SlashCommandEvent event) {

        Bot.asyncPool.submit(() -> {
            event.getChannel().sendFile(new ByteArrayInputStream(generateLB(event).toByteArray()), "leaderboard" + ".png").queue();
        });

     return event.deferReply(true);
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

    ArrayList<String> getTopUsers (Guild guild, int num) {

        ArrayList<String> topUsers = new ArrayList<>();

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        MongoCursor<Document> doc = collection.find().projection(excludeId()).sort(descending("qotwpoints")).iterator();

        int placement = 1;
        while (doc.hasNext() && placement <= num) {

            JsonObject Root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
            String discordID = Root.get("discord_id").getAsString();
            if (guild.getMemberById(discordID) == null) continue;

            try { topUsers.add(discordID);
            } catch (Exception e) { e.printStackTrace(); }

            placement++;
        }

        return topUsers;
    }

    ArrayList<BufferedImage> getAvatars (Guild guild, ArrayList<String> list) {

        ArrayList<BufferedImage> imgs = new ArrayList<>();

        for (var id : list) {

            try {

                URL avatarURL;
                if (guild.getMemberById(id) == null) avatarURL = new URL("https://cdn.discordapp.com/attachments/743073853961666563/827477442285142026/DefaultAvatar.png");
                else avatarURL = new URL(guild.retrieveMemberById(id).complete().getUser().getEffectiveAvatarUrl() + "?size=4096");
                imgs.add(ImageIO.read(avatarURL));
            } catch (IOException e) { e.printStackTrace(); continue;}
        }
        return imgs;
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

     ByteArrayOutputStream generateLB (SlashCommandEvent event) {

         int qotwPoints = new Database().getMemberInt(event.getMember(), "qotwpoints");
         boolean topTen = false;

         if (getTopUsers(event.getGuild(), 10).contains(event.getMember().getId())) topTen = true;

         BufferedImage bufferedImage;

         if (topTen) bufferedImage = new BufferedImage(LB_WIDTH, LB_HEIGHT_TOP, BufferedImage.TYPE_INT_RGB);
         else bufferedImage = new BufferedImage(LB_WIDTH, LB_HEIGHT, BufferedImage.TYPE_INT_RGB);

         Graphics2D g2d = bufferedImage.createGraphics();

         g2d.setRenderingHint(
                 RenderingHints.KEY_TEXT_ANTIALIASING,
                 RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

         g2d.setRenderingHint(
                 RenderingHints.KEY_FRACTIONALMETRICS,
                 RenderingHints.VALUE_FRACTIONALMETRICS_ON);

         g2d.drawImage(getImage("images/LeaderboardBackground.png"), 0, 0, null);

         g2d.drawImage(getAvatars(event.getGuild(), getTopUsers(event.getGuild(), 3)).get(0), 800, 468, 400, 400, null);
         g2d.drawImage(getAvatars(event.getGuild(), getTopUsers(event.getGuild(), 3)).get(1), 350, 517, 300, 300, null);
         g2d.drawImage(getAvatars(event.getGuild(), getTopUsers(event.getGuild(), 3)).get(2), 1350, 517, 300, 300, null);

         g2d.drawImage(getImage("images/LeaderboardOverlay.png"), 0, 0, null);

         g2d.setColor(Color.WHITE);
         g2d.setFont(getFont(NAME_SIZE));

         int nameY = 1188;

         for (var memberID : getTopUsers(event.getGuild(), 10)) {

             String tag = event.getGuild().getMemberById(memberID).getUser().getAsTag();

             int stringLength = (int)
                     g2d.getFontMetrics().getStringBounds(tag, g2d).getWidth();
             int start = LB_WIDTH / 2 - stringLength / 2;
             g2d.drawString(tag, start + 0, nameY);
             nameY = nameY + 180;
         }

         g2d.setFont(getFont(POINTS_SIZE));
         g2d.setColor(new Color(0xA4A4A4));

         int pointsY = 1240;

         for (var memberID : getTopUsers(event.getGuild(), 10)) {

             String points = new Database().getMemberInt(event.getGuild().getMemberById(memberID), "qotwpoints") + " points";

             int stringLength = (int)
                     g2d.getFontMetrics().getStringBounds(points, g2d).getWidth();
             int start = LB_WIDTH / 2 - stringLength / 2;
             g2d.drawString(points, start + 0, pointsY);
             pointsY = pointsY + 180;
         }

         g2d.setFont(getFont(NAME_SIZE));
         g2d.setColor(new Color(0x48494A));

         if (!topTen) {

             String text = event.getUser().getAsTag() + " - " + qotwPoints + " points";

             int stringLength = (int) g2d.getFontMetrics().getStringBounds(text, g2d).getWidth();
             int start = LB_WIDTH / 2 - stringLength / 2;
             g2d.drawString(text, start, 3095);

             int dotsStringLength = (int) g2d.getFontMetrics().getStringBounds("...", g2d).getWidth();
             int dotsStart = LB_WIDTH / 2 - dotsStringLength / 2;
             g2d.drawString("...", dotsStart, 2970);

            g2d.drawString(getQOTWRank(event.getGuild(), event.getUser().getId()) + ".", 93, 3095);
         }

         g2d.dispose();

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         try { ImageIO.write(bufferedImage, "png", outputStream);
         } catch (IOException e) { e.printStackTrace(); }

         event.getHook().sendMessage("Done!").queue();
         return outputStream;
     }
}