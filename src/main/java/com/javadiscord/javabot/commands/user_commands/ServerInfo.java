package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Misc;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.ActionRow;
import net.dv8tion.jda.api.interactions.button.Button;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class ServerInfo extends Command {

    public static void exCommand (CommandEvent event) {

        long roleCount = event.getGuild().getRoles().stream().count() - 1;
        long catCount = event.getGuild().getCategories().stream().count();
        long textChannelCount = event.getGuild().getTextChannels().stream().count();
        long voiceChannelCount = event.getGuild().getVoiceChannels().stream().count();
        long channelCount = event.getGuild().getChannels().stream().count() - catCount;

        String guildDate = event.getGuild().getTimeCreated().format(DateTimeFormatter.ofPattern("EEE',' dd/MM/yyyy',' HH:mm", new Locale("en")));
        String createdDiff = " (" + Misc.getDateDiff(Date.from(event.getGuild().getTimeCreated().toInstant()), Date.from(new Date().toInstant())) + " ago)";

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(0x2F3136))
                .setThumbnail(event.getGuild().getIconUrl())
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .addField("Name", "```" + event.getGuild().getName() + "```", true)
                .addField("Owner", "```" + event.getGuild().getOwner().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + event.getGuild().getId() + "```", false)
                .addField("Region", "```" + event.getGuild().getRegion() + "```", true)
                .addField("Roles", "```" + roleCount + " Roles```", true)
                .addField("Channel Count", "```" + channelCount + " Channel, " + catCount + " Categories" +
                        "\n → Text: " + textChannelCount +
                        "\n → Voice: " + voiceChannelCount + "```", false)

                .addField("Member Count", "```" + event.getGuild().getMemberCount() + " members```", false)
                .addField("Server created on", "```" + guildDate + createdDiff + "```", false)
                .setTimestamp(new Date().toInstant());

        if (event.getGuild().getId().equals("648956210850299986")) {
            event.reply(new MessageBuilder().setEmbed(eb.build()).setActionRows(ActionRow.of(Button.link("Website", Constants.WEBSITE))).build());
        } else { event.reply(eb.build()); }


    }

    public static void exCommand (SlashCommandEvent event) {

        long roleCount = event.getGuild().getRoles().stream().count() - 1;
        long catCount = event.getGuild().getCategories().stream().count();
        long textChannelCount = event.getGuild().getTextChannels().stream().count();
        long voiceChannelCount = event.getGuild().getVoiceChannels().stream().count();
        long channelCount = event.getGuild().getChannels().stream().count() - catCount;

        String guildDate = event.getGuild().getTimeCreated().format(DateTimeFormatter.ofPattern("EEE',' dd/MM/yyyy',' HH:mm", new Locale("en")));
        String createdDiff = " (" + Misc.getDateDiff(Date.from(event.getGuild().getTimeCreated().toInstant()), Date.from(new Date().toInstant())) + " ago)";

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(0x2F3136))
                .setThumbnail(event.getGuild().getIconUrl())
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .addField("Name", "```" + event.getGuild().getName() + "```", true)
                .addField("Owner", "```" + event.getGuild().getOwner().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + event.getGuild().getId() + "```", false)
                .addField("Region", "```" + event.getGuild().getRegion() + "```", true)
                .addField("Roles", "```" + roleCount + " Roles```", true)
                .addField("Channel Count", "```" + channelCount + " Channel, " + catCount + " Categories" +
                        "\n → Text: " + textChannelCount +
                        "\n → Voice: " + voiceChannelCount + "```", false)

                .addField("Member Count", "```" + event.getGuild().getMemberCount() + " members```", false)
                .addField("Server created on", "```" + guildDate + createdDiff + "```", false)
                .setTimestamp(new Date().toInstant());

        if (event.getGuild().getId().equals("648956210850299986")) {
            event.replyEmbeds(eb.build()).addActionRow(Button.link(Constants.WEBSITE, "Website")).queue();
        } else { event.replyEmbeds(eb.build()).queue(); }
    }

    public ServerInfo() {
            this.name = "serverinfo";
            this.aliases = new String[]{"server"};
            this.category = new Category("USER COMMANDS");
            this.help = "Shows some information about the current guild";
    }

    protected void execute(CommandEvent event) {

        exCommand(event);
    }
}