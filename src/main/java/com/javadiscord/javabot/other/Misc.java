package com.javadiscord.javabot.other;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import javax.imageio.ImageIO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;

import static com.javadiscord.javabot.events.Startup.iae;

public class Misc {

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal i = BigDecimal.valueOf(value);
        i = i.setScale(places, RoundingMode.HALF_UP);
        return i.doubleValue();
    }

    public static int parseInt (String input) {

        int i;

        try {
            i = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            i = 0;
        }

        return i;
    }

    public static String checkImage (String input) {

        try {
            ImageIO.read(new URL(input));
        } catch (Exception e) {
            input = iae;
        }

        return input;
    }

    public static boolean isImage (String input) {

        boolean b = true;

        try {
            ImageIO.read(new URL(input));
        } catch (Exception e) {
            b = false;
        }

        return b;
    }

    public static void sendToLog(Guild guild, MessageEmbed embed) {

        guild.getTextChannelById(Database.getConfigString(guild.getName(), guild.getId(), "channels.log_cid")).sendMessageEmbeds(embed).queue();
    }

    public static void sendToLog(Guild guild, String text) {

        guild.getTextChannelById(Database.getConfigString(guild.getName(), guild.getId(), "channels.log_cid")).sendMessage(text).queue();
    }

    public static String getGuildList (List<Guild> guildList, boolean showID, boolean showMemCount) {

        StringBuilder sb = new StringBuilder();
        for (int guildAmount = guildList.size(); guildAmount > 0; guildAmount--) {

            sb.append(", " + guildList.get(guildAmount - 1).getName());

                    if (showID && showMemCount) sb.append(" (" + guildList.get(guildAmount - 1).getId() + ", " + guildList.get(guildAmount - 1).getMemberCount() + " members)");
                    else if (showID && !showMemCount) sb.append(" (" + guildList.get(guildAmount - 1).getId() + ")");
                    else if (!showID && showMemCount) sb.append(" (" + guildList.get(guildAmount - 1).getMemberCount() + " members)");
        }

        return sb.substring(2);
    }

    /*public static MessageEmbed HelpEmbed (CommandEvent event) {

        Command.Category category = null;
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Command List")
                .setColor(Constants.GRAY)
                .setThumbnail(event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        StringBuilder builder = new StringBuilder("**" + event.getJDA().getSelfUser().getName() + "** - currently running version ``" + new Version().getVersion() + "``\n\n" +
                "``<>`` - required\n``()`` - optional");

        for(Command command : event.getClient().getCommands()) {

            if(command.isHidden()) continue;
            if(command.isOwnerCommand() && !event.getAuthor().getId().equals(event.getClient().getOwnerId())) continue;

            if(!isCategoryEqual(category, command.getCategory())) {

                category = command.getCategory();
                builder.append("\n\n**").append(category == null ? "No Category" : category.getName()).append("**\n");
            }

            builder.append("\n``• ").append(event.getClient().getPrefix()).append(command.getName())
                    .append(command.getArguments() == null ? "``" : " " + command.getArguments()+"``")
                    //.append(" - ").append(command.getHelp())
                    ;
        }

        String desc = builder.toString();
        eb.setDescription(desc);

        return eb.build();
    }

    public static boolean isCategoryEqual(Command.Category first, Command.Category second) {

        if(first == null) return second == null;
        if(second == null) return false;
        return first.getName().equals(second.getName());
    }*/
}
