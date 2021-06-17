package com.javadiscord.javabot.commands.other.qotw;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Indexes.descending;
import static com.mongodb.client.model.Projections.excludeId;

public class Leaderboard implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        boolean old;
        try {
            old = event.getOption("old").getAsBoolean();
        } catch (NullPointerException e) {
            old = false;
        }
        if (old) generateOldLB(event);
        else generateLB(event);
    }

    static void generateLB(SlashCommandEvent event) {

        event.deferReply(false).queue();
        InteractionHook hook = event.getHook();

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        int QOTWPoints = Database.getMemberInt(collection, event.getMember(), "qotwpoints");

        ArrayList<String> topTenID = new ArrayList<String>();
        ArrayList<String> topTenAvatarURL = new ArrayList<String>();
        ArrayList<String> topTenName = new ArrayList<String>();
        ArrayList<String> topTenPoints = new ArrayList<String>();

        String avatarURL;
        String memberName;

        MongoCursor<Document> doc = collection.find().projection(excludeId()).sort(descending("qotwpoints")).iterator();
        int placement = 1;
        while (doc.hasNext() && placement <= 10) {

            JsonObject Root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
            String discordID = Root.get("discord_id").getAsString();
            topTenID.add(discordID);

            try {
                avatarURL = event.getGuild().getMemberById(discordID).getUser().getEffectiveAvatarUrl();
            } catch (NullPointerException e) {
                avatarURL = "https://cdn.discordapp.com/attachments/743073853961666563/827477442285142026/DefaultAvatar.png";
            }
            topTenAvatarURL.add(avatarURL);

            try {
                memberName = event.getGuild().getMemberById(discordID).getUser().getAsTag();
            } catch (NullPointerException e) {
                memberName = Root.get("tag").getAsString();
            }
            topTenName.add(memberName);

            String points = Root.get("qotwpoints").getAsString();
            topTenPoints.add(points);

            placement++;
        }

        boolean topTen = false;
        if(topTenID.contains(event.getUser().getId())) {
           topTen = true;
        }

        int stringWidth;
        int width = 2000;

        int height = 3200;

        if(topTen) {
            height = 3000;
        }

        float listNameSize = 65;
        float listPointsSize = 40;


        BufferedImage backgroundImage = null;
        try {
            backgroundImage = ImageIO.read(Leaderboard.class.getClassLoader().getResourceAsStream("images/LeaderboardBackground.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedImage overlayImage = null;
        try {
            overlayImage = ImageIO.read(Leaderboard.class.getClassLoader().getResourceAsStream("images/LeaderboardOverlay.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedImage fpAvatarImage = null;
        BufferedImage spAvatarImage = null;
        BufferedImage tpAvatarImage = null;

        try {
            URL fpAvatarURL = new URL(topTenAvatarURL.get(0) + "?size=4096");
            fpAvatarImage = ImageIO.read(fpAvatarURL);

            URL spAvatarURL = new URL(topTenAvatarURL.get(1) + "?size=4096");
            spAvatarImage = ImageIO.read(spAvatarURL);

            URL tpAvatarURL = new URL(topTenAvatarURL.get(2) + "?size=4096");
            tpAvatarImage = ImageIO.read(tpAvatarURL);

        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        Font listNameFont = null;
        try {
            listNameFont = Font.createFont(Font.TRUETYPE_FONT, Leaderboard.class.getClassLoader().getResourceAsStream("fonts/Uni-Sans-Heavy.ttf")).deriveFont(listNameSize);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            ge.registerFont(listNameFont);
        } catch (IOException | FontFormatException e) {
            listNameFont = new Font("Arial", Font.PLAIN, 120);
        }

        Font listPointsFont = null;
        try {
            listPointsFont = Font.createFont(Font.TRUETYPE_FONT, Leaderboard.class.getClassLoader().getResourceAsStream("fonts/Uni-Sans-Heavy.ttf")).deriveFont(listPointsSize);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            ge.registerFont(listPointsFont);
        } catch (IOException | FontFormatException e) {
            listPointsFont = new Font("Arial", Font.PLAIN, 120);
        }

        g2d.drawImage(backgroundImage, 0, 0, null);

        g2d.drawImage(fpAvatarImage, 800, 468, 400, 400, null);
        g2d.drawImage(spAvatarImage, 350, 517, 300, 300, null);
        g2d.drawImage(tpAvatarImage, 1350, 517, 300, 300, null);

        g2d.drawImage(overlayImage, 0, 0, null);

        g2d.setColor(Color.white);
        g2d.setFont(listNameFont);


        int NameY = 1188;
        for (int i = 0; i < topTenName.size(); i++) {
            int stringLength = (int)
                    g2d.getFontMetrics().getStringBounds(topTenName.get(i), g2d).getWidth();
            int start = width / 2 - stringLength / 2;
            g2d.drawString(topTenName.get(i), start + 0, NameY);
            NameY = NameY + 180;
        }

        g2d.setFont(listPointsFont);
        g2d.setColor(new Color(0xA4A4A4));

        int pointsY = 1240;
        for (int i = 0; i < topTenPoints.size(); i++) {
            int stringLength = (int)
                    g2d.getFontMetrics().getStringBounds(topTenPoints.get(i) + " points", g2d).getWidth();
            int start = width / 2 - stringLength / 2;
            g2d.drawString(topTenPoints.get(i) + " points", start + 0, pointsY);
            pointsY = pointsY + 180;
        }


        g2d.setFont(listNameFont);
        g2d.setColor(new Color(0x48494A));

        if (!topTen) {


            String text = event.getUser().getAsTag() + " - " + QOTWPoints + " points (#" + rank(event.getUser().getId()) + ")";
            int stringLength = (int) g2d.getFontMetrics().getStringBounds(text, g2d).getWidth();
            int start = width / 2 - stringLength / 2;
            g2d.drawString(text, start, 3095);

            int dotsStringLength = (int) g2d.getFontMetrics().getStringBounds("...", g2d).getWidth();
            int dotsStart = width / 2 - dotsStringLength / 2;
            g2d.drawString("...", dotsStart, 2970);

            //g2d.drawString(rank(event.getAuthor().getId()) + ".", 93, 3095);
        }

        g2d.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "png", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        hook.sendFile(new ByteArrayInputStream(outputStream.toByteArray()), "leaderboard" + ".png").queue();
    }

    static void generateOldLB(SlashCommandEvent event) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        int qotwPoints = Database.getMemberInt(collection, event.getMember(), "qotwpoints");

        StringBuilder sb = new StringBuilder();
        MongoCursor<Document> doc = collection.find().projection(excludeId()).sort(descending("qotwpoints")).iterator();

        for (int i = 1; doc.hasNext() && i <= 10; i++) {

            JsonObject Root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
            String Points = Root.get("qotwpoints").getAsString();
            String Name = Root.get("tag").getAsString();
            sb.append(i + ". " + Name + " - " + Points + " points\n");
        }

        String topTen = sb.toString().replace(event.getUser().getAsTag(), "** ➔ " + event.getUser().getAsTag() + "**");
        String authorPlacement = rank(event.getUser().getId()) + ". " + event.getUser().getAsTag() + " - " + qotwPoints + " points";

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("QOTW-Leaderboard", null, event.getUser().getEffectiveAvatarUrl())
                .setColor(new Color(0x2F3136))
                .setDescription(topTen.replace("- 1 points", "- 1 point"))
                .addField("Your placement", authorPlacement.replace("- 1 points", "- 1 point"), false);

        event.replyEmbeds(eb.build()).queue();
    }

    public static int rank(String userid) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        ArrayList<String> users = new ArrayList<String>();
        MongoCursor<Document> doc = collection.find().projection(excludeId()).sort(descending("qotwpoints")).iterator();

        while (doc.hasNext()) {

            JsonObject Root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
            String discordID = Root.get("discord_id").getAsString();
            users.add(discordID);
        }

        return (users.indexOf(userid)) + 1;

    }
}