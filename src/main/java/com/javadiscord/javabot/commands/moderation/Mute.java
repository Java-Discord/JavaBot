package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.Date;


public class Mute extends Command {

    public static void mute(Member member, String moderatorTag, Object ev) {

        Guild guild = null;
        TextChannel tc = null;

        if (ev instanceof com.jagrosh.jdautilities.command.CommandEvent) {
            com.jagrosh.jdautilities.command.CommandEvent event = (CommandEvent) ev;

            guild = event.getGuild();
            tc = event.getTextChannel();
        }

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            guild = event.getGuild();
            tc = event.getChannel();
        }

        Object event = ev;


        try {

            Role muteRole = Database.configRole(event, "mute_rid");

            if (!(member.getRoles().toString().contains(muteRole.getId()))) {
                guild.addRoleToMember(member.getId(), muteRole).complete();

                EmbedBuilder eb = new EmbedBuilder()
                        .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                        .setColor(Constants.RED)
                        .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                        .addField("Moderator", "```" + moderatorTag + "```", true)
                        .addField("ID", "```" + member.getId() + "```", false)
                        .setFooter("ID: " + member.getId())
                        .setTimestamp(new Date().toInstant());

                Misc.sendToLog(event, eb.build());
                member.getUser().openPrivateChannel().complete().sendMessage(eb.build()).queue();
                tc.sendMessage(eb.build()).queue();

            }

            } catch(HierarchyException e) { tc.sendMessage(Embeds.hierarchyError(event)).queue();
            } catch(NullPointerException | NumberFormatException e) { tc.sendMessage(Embeds.syntaxError("mute @User/ID", event)); }
    }

        public Mute () { this.name = "mute"; }

        protected void execute(CommandEvent event) {
            if (event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
                String[] args = event.getArgs().split("\\s+");

                try {

                    Member member;

                    if (args.length >= 1) {
                        if (!event.getMessage().getMentionedMembers().isEmpty()) {
                            member = event.getMessage().getMentionedMembers().get(0);

                        } else {
                            member = event.getGuild().getMemberById(args[0]);
                        }

                        if (event.getMessage().getMember().equals(member)) {
                            event.reply(Embeds.selfPunishError("mute", event));
                            return;
                        }

                        mute(member, event.getAuthor().getAsTag(), event);
                    }

                } catch (NullPointerException | IllegalArgumentException e) { event.reply(Embeds.syntaxError("mute @User/ID", event)); }
                } else { event.reply(Embeds.permissionError("MANAGE_ROLES", event)); }
        }
    }