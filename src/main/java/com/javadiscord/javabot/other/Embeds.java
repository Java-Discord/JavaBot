package com.javadiscord.javabot.other;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.time.Instant;

import static com.javadiscord.javabot.events.Startup.iae;

public class Embeds {

    public static MessageEmbed permissionError(String perm, Object ev) {
        return createErrorEmbed("Sorry, {{mention}}\nTo execute this command you need the **``" + perm + "``** permission.", ev);
    }

    public static MessageEmbed purgeError(Object ev) {
        return createErrorEmbed("```!purge 2-100```\n Please note that messages older than **14 days** cannot be deleted.", ev);
    }

    public static MessageEmbed selfPunishError(String ex, Object ev) {
        return createErrorEmbed("Why would you want to " + ex + " yourself? :^)```", ev);
    }

    private static MessageEmbed createErrorEmbed(String text, Object ev) {
        User user = null;

        if (ev instanceof GuildMessageReceivedEvent event) {
            event.getMessage().addReaction(Constants.CROSS).complete();
            user = event.getAuthor();
        }

        if (ev instanceof SlashCommandEvent sev) {
            user = sev.getUser();
        }

        return emptyError(text.replace("{{mention}}", user.getAsMention()), user);
    }

    public static MessageEmbed emptyError(String text, User user) {
        return new EmbedBuilder()
                .setTitle("An Error occurred")
                .setColor(Constants.RED)
                .setDescription(text)
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();
    }

    public static MessageEmbed configEmbed(Object ev, String title, String description, String imageLink, String value, boolean showValue, boolean channel, boolean role) {

        EmbedBuilder eb = new EmbedBuilder();
        User user = null;
        Guild guild = null;

        if (ev instanceof SlashCommandEvent event) {

            user = event.getUser();
            guild = event.getGuild();
        }

        if (ev instanceof GuildMessageReceivedEvent event) {

            user = event.getAuthor();
            guild = event.getGuild();
        }

        if (channel) value = guild.getTextChannelById(value).getAsMention();
        if (role) value = guild.getRoleById(value).getAsMention();

        if (!role && !channel) value = "``" + value + "``";

        eb.setAuthor("Config: " + title);

        try {
            eb.setImage(imageLink);
        } catch (IllegalArgumentException e) {
            eb.setImage(iae);
        }

        eb.setColor(Constants.GRAY);

                if (showValue) eb.setDescription(description + " " + value);
                else eb.setDescription(description);

                eb.setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now());

        return eb.build();
    }

    public static MessageEmbed configEmbed(Object ev, String title, String description, String imageLink, String value, boolean showValue, boolean channel) {
        return Embeds.configEmbed(ev, title, description, imageLink, value, showValue, channel, false);
    }

    public static MessageEmbed configEmbed (Object ev, String title, String description, String imageLink, String value, boolean showValue) {
        return Embeds.configEmbed(ev, title, description, imageLink, value, showValue, false, false);
    }

    public static MessageEmbed configEmbed (Object ev, String title, String description, String imageLink, String value) {
        return Embeds.configEmbed(ev, title, description, imageLink, value, false, false, false);
    }
}

