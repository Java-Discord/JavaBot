package Commands.Moderation;

import Other.Database;
import Other.Embeds;
import Other.Misc;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.bson.Document;

import java.awt.*;
import java.util.Arrays;
import java.util.Date;

import static Events.Startup.mongoClient;

public class Warn extends Command {

    public static void warn(Member member, String reason, String moderatorTag, Object ev) {

        TextChannel tc = null;
        SelfUser selfUser = null;

        if (ev instanceof com.jagrosh.jdautilities.command.CommandEvent) {
            com.jagrosh.jdautilities.command.CommandEvent event = (CommandEvent) ev;

            tc = event.getTextChannel();
            selfUser = event.getSelfUser();

        }

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            tc = event.getChannel();
            selfUser = event.getJDA().getSelfUser();
        }

        Object event = ev;

        try {

            MongoDatabase database = mongoClient.getDatabase("userdata");
            MongoCollection<Document> collection = database.getCollection("users");

            int warnPoints = Database.getMemberInt(collection, member, "warns");
            int totalWarnPoints = warnPoints + 1;
            Database.queryMemberInt(member.getId(), "warns", totalWarnPoints);

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag() + " | Warn (" + totalWarnPoints + "/3)", null, member.getUser().getEffectiveAvatarUrl())
                    .setColor(Color.YELLOW)
                    .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                    .addField("Moderator", "```" + moderatorTag + "```", true)
                    .addField("ID", "```" + member.getId() + "```", false)
                    .addField("Reason", "```" + reason + "```", false)
                    .setFooter("ID: " +  member.getId())
                    .setTimestamp(new Date().toInstant());

            member.getUser().openPrivateChannel().complete().sendMessage(eb.build()).queue();
            Misc.sendToLog(event, eb.build());
            tc.sendMessage(eb.build()).queue();

            if (totalWarnPoints >= 3) {
                Database.queryMemberInt(member.getId(), "warns", 0);
                new Ban().ban(member, "3/3 warns", selfUser.getAsTag(), event);
            }

        } catch (HierarchyException e) { tc.sendMessage(Embeds.hierarchyError(event)).queue();
        } catch (NullPointerException | NumberFormatException e) { tc.sendMessage(Embeds.syntaxError("warn @User/ID (Reason)", event)); }
    }

    public Warn () { this.name = "warn"; }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.KICK_MEMBERS)) {

            String[] args = event.getArgs().split("\\s+");

                try {
                    Member member = null;
                    String reason;

                    if (args.length >= 1) {
                        if (!event.getMessage().getMentionedMembers().isEmpty()) {
                            member = event.getMessage().getMentionedMembers().get(0);

                        } else { member = event.getGuild().getMemberById(args[0]); }

                        if (event.getMessage().getMember().equals(member)) {
                            event.reply(Embeds.selfPunishError("warn", event));
                            return;
                        }
                    }

                    if (args.length >= 2) {
                        String[] Arg = Arrays.copyOfRange(args, 1, args.length);
                     StringBuilder builder = new StringBuilder();

                        for (String value : Arg) {
                         builder.append(value + " ");
                        }

                        reason = builder.substring(0, builder.toString().length() - 1);

                } else { reason = "None"; }

                warn(member, reason, event.getAuthor().getAsTag(), event);

                } catch (NullPointerException | IllegalArgumentException e) { event.reply(Embeds.syntaxError("warn @User/ID (Reason)", event)); }
                } else { event.reply(Embeds.permissionError("KICK_MEMBERS", event)); }
        }
        }

