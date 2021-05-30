package com.javadiscord.javabot.commands.user_commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;

public class Avatar extends Command {

    public static void exCommand (CommandEvent event) {

        String[] args = event.getArgs().split("\\s+");

        Member member = event.getMember();

        if (args.length == 1) {

            if (!event.getMessage().getMentionedMembers().isEmpty()) {
                member = event.getGuild().getMember(event.getMessage().getMentionedUsers().get(0));

            } else {
                try {
                    member = event.getGuild().getMember(event.getJDA().getUserById(args[0]));
                } catch (IllegalArgumentException e) {
                    member = event.getGuild().getMember(event.getMessage().getAuthor());
                }
            }
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Avatar")
                .setColor(new Color(0x2F3136))
                .setImage(member.getUser().getEffectiveAvatarUrl() + "?size=4096");

        event.reply(eb.build());

    }

    public static void exCommand (SlashCommandEvent event, User user) {

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(user.getAsTag() + " | Avatar")
                .setColor(new Color(0x2F3136))
                .setImage(user.getEffectiveAvatarUrl() + "?size=4096");

        event.replyEmbeds(eb.build()).queue();

    }

    public Avatar() {
        this.name = "avatar";
        this.aliases = new String[]{ "av", "pic", "picture" };
        this.category = new Category("USER COMMANDS");
        this.arguments = "(@User/ID)";
        this.help = "Shows your profile picture";
    }

    @Override
    protected void execute(CommandEvent event) {

        exCommand(event);
    }
}
