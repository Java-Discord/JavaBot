package com.javadiscord.javabot.events;

import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.ServerLock;
import com.javadiscord.javabot.other.StatsCategory;
import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static com.javadiscord.javabot.events.Startup.iae;

public class UserJoin extends ListenerAdapter {

    public byte[] generateImage(Object ev, boolean forceFlag, boolean forceBot) throws MalformedURLException {

        Guild guild = null;
        Member member = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            guild = event.getGuild();
            member = event.getMember();
        }

        if (ev instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent event = (GuildMemberJoinEvent) ev;

            guild = event.getGuild();
            member = event.getMember();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            guild = event.getGuild();
            member = event.getMember();
        }

        Database db = new Database();

        int stringWidth;
        int imgW = db.getConfigInt(guild, "welcome_system.image.imgW");
        int imgH = db.getConfigInt(guild, "welcome_system.image.imgH");
        int avX = db.getConfigInt(guild, "welcome_system.image.avatar.avX");
        int avY = db.getConfigInt(guild, "welcome_system.image.avatar.avY");
        int avW = db.getConfigInt(guild, "welcome_system.image.avatar.avW");
        int avH = db.getConfigInt(guild, "welcome_system.image.avatar.avH");

        int primCol = db.getConfigInt(guild, "welcome_system.image.primCol");
        int secCol = db.getConfigInt(guild, "welcome_system.image.secCol");

        float memberSize = 120;
        float countSize = 72;

        Font CountFont, MemberFont;
        URL overlayURL, bgURL, avatarURL;
        BufferedImage flagImage = null, botImage = null, avatarImage = null, bgImage = null, overlayImage = null;

        try {
            overlayURL = new URL(db.getConfigString(guild, "welcome_system.image.overlayURL"));
        } catch (MalformedURLException e) {
            overlayURL = new URL(iae);
        }

        try {
            bgURL = new URL(db.getConfigString(guild, "welcome_system.image.bgURL"));
        } catch (MalformedURLException e) {
            bgURL = new URL(iae);
        }

        try {
            overlayImage = ImageIO.read(overlayURL);
        } catch (IOException e) { e.printStackTrace();}


        try {
            bgImage = ImageIO.read(bgURL);
        } catch (IOException e) { e.printStackTrace();}

            BufferedImage bufferedImage = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            try {

                avatarURL = new URL(member.getUser().getEffectiveAvatarUrl() + "?size=4096");
                avatarImage = ImageIO.read(avatarURL);
                botImage = ImageIO.read(UserJoin.class.getClassLoader().getResourceAsStream("images/BotIcon.png"));
                flagImage = ImageIO.read(UserJoin.class.getClassLoader().getResourceAsStream("images/FlagIcon.png"));

            } catch (IOException e) { e.printStackTrace(); }

            try {
                MemberFont = Font.createFont(Font.TRUETYPE_FONT, UserJoin.class.getClassLoader().getResourceAsStream("fonts/Uni-Sans-Heavy.ttf")).deriveFont(memberSize);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

                ge.registerFont(MemberFont);
            } catch (IOException | FontFormatException e) {
                MemberFont = new Font("Arial", Font.PLAIN, 120);
            }

            try {
                CountFont = Font.createFont(Font.TRUETYPE_FONT, UserJoin.class.getClassLoader().getResourceAsStream("fonts/Uni-Sans-Heavy.ttf")).deriveFont(countSize);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

                ge.registerFont(CountFont);
            } catch (IOException | FontFormatException e) {
                CountFont = new Font("Arial", Font.PLAIN, 120);
            }

            g2d.drawImage(bgImage, 0, 0, null);
            g2d.drawImage(avatarImage, avX, avY, avW, avH, null);
            g2d.drawImage(overlayImage, 0, 0, null);

            g2d.setColor(new Color(primCol));
            g2d.setFont(MemberFont);

            stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getAsTag());

            while (stringWidth > 1300) {

                Font currentFont = g2d.getFont();
                Font newFont = currentFont.deriveFont(currentFont.getSize() - 1F);
                g2d.setFont(newFont);
                stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getAsTag());
            }

            g2d.drawString(member.getUser().getAsTag(), 550, 305);

            g2d.setColor(new Color(secCol));
            g2d.setFont(CountFont);
            g2d.drawString("Member #" + guild.getMemberCount(), 550, 380);

            if (member.getUser().isBot() || forceBot) {
                g2d.drawImage(botImage, 340, 400, null);
            }

            if (member.getUser().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7)) || forceFlag) {
                g2d.drawImage(flagImage, 340, 350, null);
            }

            g2d.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                ImageIO.write(bufferedImage, "png", outputStream);
            } catch (IOException e) { e.printStackTrace(); }

            return outputStream.toByteArray();
        }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getMember().getUser().isBot()) return;

        User user = event.getMember().getUser();
        TextChannel welcomeChannel = new Database().getConfigChannel(event.getGuild(), "welcome_system.welcome_cid");

        if (!ServerLock.lockStatus(event)) {

            ServerLock.checkLock(event, user);

            CompletableFuture.runAsync(() -> {

                try {

                    if (new Database().getConfigBoolean(event.getGuild(), "welcome_system.welcome_status")) {

                        String welcomeMessage = new Database().getConfigString(event.getGuild(), "welcome_system.join_msg")
                                .replace("{!member}", event.getMember().getAsMention())
                                .replace("{!membertag}", event.getMember().getUser().getAsTag())
                                .replace("{!server}", event.getGuild().getName());

                        event.getGuild().getTextChannelById(new Database().getConfigString(event.getGuild(), "welcome_system.welcome_cid")).sendMessage(welcomeMessage)
                                .addFile(new ByteArrayInputStream(generateImage(event, false, false)), event.getMember().getId() + ".png").queue();
                    }

                    StatsCategory.update(event.getGuild());

                } catch (Exception e) { e.printStackTrace(); }
            });


        } else {

            user.openPrivateChannel().complete().sendMessage(ServerLock.lockEmbed(event.getGuild())).queue();
            event.getMember().kick().complete();

            String diff = new TimeUtils().formatDurationToNow(event.getMember().getTimeCreated());
            welcomeChannel.sendMessage("**" + event.getMember().getUser().getAsTag() + "**" + " (" + diff + " old) tried to join this server.").queue();

        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getMember().getUser().isBot() || event.getMember() == null) return;

        String[] args = event.getMessage().getContentDisplay().split(" ");

        if (args[0].equalsIgnoreCase("!generateImage") && event.getMember().getId().equals("810481402390118400")) {

            boolean imgFlag = false;
            boolean imgBot = false;

            if (args.length > 1) {
                switch (args[1]) {
                    case "imgBot":
                        imgBot = true;
                        break;
                    case "imgFlag":
                        imgFlag = true;
                        break;
                    default:
                }
            }

            boolean finalImgFlag = imgFlag;
            boolean finalImgBot = imgBot;

            CompletableFuture.runAsync(() -> {

                try {
                    event.getChannel().sendFile(new ByteArrayInputStream(generateImage(event, finalImgFlag, finalImgBot)), event.getMember().getId() + ".png").queue();
                }
                catch (MalformedURLException e) { e.printStackTrace(); }
            });
        }
    }
}



