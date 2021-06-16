package com.javadiscord.javabot.commands.other.qotw;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.bson.Document;

import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class Correct extends Command {

    public Correct() {
        this.name = "correct";
        this.category = new Category("MODERATION");
        this.arguments = "<ID>";
        this.help = "grants the given user one qotw-point";
    }

    public static void correct(Object ev, Member member) {

        String check = null;
        TextChannel tc = null;

        if (ev instanceof com.jagrosh.jdautilities.command.CommandEvent) {
            com.jagrosh.jdautilities.command.CommandEvent event = (CommandEvent) ev;

            tc = event.getTextChannel();
            check = event.getGuild().getEmotesByName("check", false).get(0).getAsMention();

        }

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent) {
            net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent event = (GuildMessageReactionAddEvent) ev;


            tc = event.getGuild().getTextChannelById(Database.getConfigString(event.getGuild().getName(), event.getGuild().getId(), "log_cid"));
            check = event.getGuild().getEmotesByName("check", false).get(0).getAsMention();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.ButtonClickEvent) {
            net.dv8tion.jda.api.events.interaction.ButtonClickEvent event = (ButtonClickEvent) ev;


            tc = event.getGuild().getTextChannelById(Database.getConfigString(event.getGuild().getName(), event.getGuild().getId(), "log_cid"));
            check = event.getGuild().getEmotesByName("check", false).get(0).getAsMention();
        }

            MongoDatabase database = mongoClient.getDatabase("userdata");
            MongoCollection<Document> collection = database.getCollection("users");

            int qotwPoints = Database.getMemberInt(collection, member, "qotwpoints");
            Database.queryMemberInt(member.getId(), "qotwpoints", qotwPoints + 1);

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor("Question of the Week", null, member.getUser().getEffectiveAvatarUrl())
                    .setColor(Constants.GREEN)
                    .setDescription("Your answer was correct! " + check + "\nYou've been granted **1 QOTW-Point!** (Total: " + (qotwPoints + 1) + ")")
                    .setTimestamp(new Date().toInstant());

            try {
                member.getUser().openPrivateChannel().complete().sendMessage(eb.build()).queue();

                EmbedBuilder emb = new EmbedBuilder()
                        .setAuthor(member.getUser().getAsTag() + " | QOTW-Point added", null, member.getUser().getEffectiveAvatarUrl())
                        .setColor(Constants.GREEN)
                        .addField("Total QOTW-Points", "```" + (qotwPoints + 1) + "```", true)
                        .addField("Rank", "```#" + Leaderboard.rank(member.getId()) + "```", true)
                        .setFooter("ID: " + member.getId())
                        .setTimestamp(new Date().toInstant());
                tc.sendMessage(emb.build()).queue();

            } catch (Exception e) {
                tc.sendMessage(Embeds.emptyError("```Couldn't send message <:abort:759740784882089995> (" + member.getUser().getAsTag() + ")```", ev)).queue();
            }
    }


    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");
            Member member = null;

            if (args.length >= 1) {
                if (!event.getMessage().getMentionedMembers().isEmpty()) {
                    member = event.getMessage().getMentionedMembers().get(0);
                } else {
                    member = event.getGuild().getMemberById(args[0]);
                }
            }

            correct(event, member);

        } else {
            event.reply(Embeds.permissionError("MESSAGES_MANAGE", event));
        }

    }
}
