package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.other.ServerLock;
import com.javadiscord.javabot.other.StatsCategory;
import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UserJoin extends ListenerAdapter {

    public static final float NAME_SIZE = 120;
    public static final float MEMBERCOUNT_SIZE = 72;
    public static final float MAX_STRING_SIZE = 1300;

    public byte[] generateImage(Guild guild, Member member) throws Exception {

        var config = Bot.config.get(guild).getWelcome();
        var avatarConfig = config.getImageConfig().getAvatarConfig();

        URL overlayURL = new URL(config.getImageConfig().getOverlayImageUrl());
        URL bgURL = new URL(config.getImageConfig().getBackgroundImageUrl());

        BufferedImage overlayImage = ImageIO.read(overlayURL);
        BufferedImage bgImage = ImageIO.read(bgURL);

        BufferedImage bufferedImage = new BufferedImage(config.getImageConfig().getWidth(),
                config.getImageConfig().getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        URL avatarURL = new URL(member.getUser().getEffectiveAvatarUrl() + "?size=4096");
        BufferedImage avatarImage = ImageIO.read(avatarURL);
        BufferedImage botIcon = ImageIO.read(Objects.requireNonNull(UserJoin.class.getClassLoader().getResourceAsStream("images/BotIcon.png")));

        Font memberCountFont, nameFont;

        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            nameFont = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(UserJoin.class.getClassLoader()
                    .getResourceAsStream("fonts/Uni-Sans-Heavy.ttf"))).deriveFont(NAME_SIZE);
            memberCountFont = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(UserJoin.class.getClassLoader()
                    .getResourceAsStream("fonts/Uni-Sans-Heavy.ttf"))).deriveFont(MEMBERCOUNT_SIZE);

            ge.registerFont(nameFont);
            ge.registerFont(memberCountFont);
        } catch (IOException | FontFormatException e) {
            nameFont = new Font("Arial", Font.PLAIN, (int) NAME_SIZE);
            memberCountFont = new Font("Arial", Font.PLAIN, (int) MEMBERCOUNT_SIZE);
        }

        g2d.drawImage(bgImage, 0, 0, null);
        g2d.drawImage(avatarImage, avatarConfig.getX(), avatarConfig.getY(), avatarConfig.getWidth(), avatarConfig.getHeight(), null);
        g2d.drawImage(overlayImage, 0, 0, null);

        g2d.setColor(new Color(config.getImageConfig().getPrimaryColor()));
        g2d.setFont(nameFont);

        int stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getAsTag());

        while (stringWidth > MAX_STRING_SIZE) {
            Font currentFont = g2d.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() - 1F);
            g2d.setFont(newFont);
            stringWidth = g2d.getFontMetrics().stringWidth(member.getUser().getAsTag());
        }

        g2d.drawString(member.getUser().getAsTag(), 550, 305);

        g2d.setColor(new Color(config.getImageConfig().getSecondaryColor()));
        g2d.setFont(memberCountFont);
        g2d.drawString("Member #" + guild.getMemberCount(), 550, 380);

        if (member.getUser().isBot()) {
            g2d.drawImage(botIcon, 340, 400, null);
        }
        g2d.dispose();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try { ImageIO.write(bufferedImage, "png", outputStream);
        } catch (IOException e) { e.printStackTrace(); }
        return outputStream.toByteArray();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getMember().getUser().isBot()) return;

        User user = event.getMember().getUser();
        var welcomeConfig = Bot.config.get(event.getGuild()).getWelcome();
        TextChannel welcomeChannel = event.getGuild().getTextChannelById(welcomeConfig.getChannelId());

        if (!ServerLock.lockStatus(event)) {
            ServerLock.checkLock(event, user);
            CompletableFuture.runAsync(() -> {

                if (welcomeConfig.isEnabled()) {
                    String welcomeMessage = Objects.requireNonNull(welcomeConfig.getJoinMessageTemplate())
                            .replace("{!member}", event.getMember().getAsMention())
                            .replace("{!membertag}", event.getMember().getUser().getAsTag())
                            .replace("{!server}", event.getGuild().getName());

                    try {
                    welcomeConfig.getChannel().sendMessage(welcomeMessage)
                            .addFile(new ByteArrayInputStream(generateImage(event.getGuild(), event.getMember())), event.getMember().getId() + ".png").queue();
                    } catch (Exception e) { e.printStackTrace(); }
                }
                StatsCategory.update(event.getGuild());
            });
        } else {
            if (user.hasPrivateChannel()) user.openPrivateChannel().complete()
                    .sendMessageEmbeds(ServerLock.lockEmbed(event.getGuild())).queue();
            event.getMember().kick().complete();

            String diff = new TimeUtils().formatDurationToNow(event.getMember().getTimeCreated());
            welcomeChannel.sendMessage("**" + event.getMember().getUser().getAsTag() + "**" + " (" + diff + " old) tried to join this server.").queue();
        }
    }
}



