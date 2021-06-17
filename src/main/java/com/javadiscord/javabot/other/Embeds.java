package com.javadiscord.javabot.other;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Date;

import static com.javadiscord.javabot.events.Startup.iae;

public class Embeds {

    public static MessageEmbed permissionError(String perm, Object ev) {

        User user = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            event.getMessage().addReaction(Constants.CROSS).complete();
            user = event.getAuthor();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("An Error occurred")
                .setColor(Constants.RED)
                .setDescription("Sorry, " + user.getAsMention() + "\nTo execute this command you need following permissions: **``" + perm + "``**")
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed syntaxError(String syntax, Object ev) {

        User user = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            event.getMessage().addReaction(Constants.CROSS).complete();
            user = event.getAuthor();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("An Error occurred")
                .setColor(Constants.RED)
                .setDescription("```" + "!" + syntax + "```")
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed hierarchyError(Object ev) {

        User user = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            event.getMessage().addReaction(Constants.CROSS).complete();
            user = event.getAuthor();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("An Error occurred")
                .setColor(Constants.RED)
                .setDescription("```Can't modify a member with higher or equal highest role than yourself!```")
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed purgeError(Object ev) {

        User user = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            event.getMessage().addReaction(Constants.CROSS).complete();
            user = event.getAuthor();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
        }


        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("An Error occurred")
                .setColor(Constants.RED)
                .setDescription("```" + "!" + "purge 2-100" + "```\nPlease note that messages that are older than **14 days** cannot be deleted.")
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed selfPunishError(String ex, Object ev) {

        User user = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            event.getMessage().addReaction(Constants.CROSS).complete();
            user = event.getAuthor();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("An Error occurred")
                .setColor(Constants.RED)
                .setDescription("```Why would you want to " + ex + " yourself? :^)```")
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed emptyError(String text, Object ev) {

        User user = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            event.getMessage().addReaction(Constants.CROSS).complete();
            user = event.getAuthor();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("An Error occurred")
                .setColor(Constants.RED)
                .setDescription(text)
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed emptyEmbed(String title, String desc, String image, Object ev) {

        User user = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            user = event.getAuthor();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setImage(image)
                .setColor(Constants.RED)
                .setDescription(desc)
                .setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed configEmbed (Object ev, String title, String description, String imageLink, String value, boolean showValue, boolean channel, boolean role) {

        EmbedBuilder eb = new EmbedBuilder();
        User user = null;
        Guild guild = null;

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            user = event.getUser();
            guild = event.getGuild();
        }

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

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
                .setTimestamp(new Date().toInstant());

        return eb.build();
    }

    public static MessageEmbed configEmbed (Object ev, String title, String description, String imageLink, String value, boolean showValue, boolean channel) {

        return Embeds.configEmbed(ev, title, description, imageLink, value, showValue, channel, false);
    }

    public static MessageEmbed configEmbed (Object ev, String title, String description, String imageLink, String value, boolean showValue) {

        return Embeds.configEmbed(ev, title, description, imageLink, value, showValue, false, false);
    }

    public static MessageEmbed configEmbed (Object ev, String title, String description, String imageLink, String value) {

        return Embeds.configEmbed(ev, title, description, imageLink, value, false, false, false);
    }

}

