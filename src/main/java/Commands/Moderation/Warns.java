package Commands.Moderation;

import Other.Constants;
import Other.Database;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.bson.Document;

import java.awt.*;
import java.util.Date;

import static Events.Startup.mongoClient;

public class Warns extends Command {

    public Warns () { this.name = "warns"; }

    protected void execute(CommandEvent event) {

            String[] args = event.getArgs().split("\\s+");
            Member member = event.getMember();

            MongoDatabase database = mongoClient.getDatabase("userdata");
            MongoCollection<Document> collection = database.getCollection("users");

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

            int warnCount = Database.getMemberInt(collection, member, "warns");

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag() + " | Warns", null, member.getUser().getEffectiveAvatarUrl())
                    .setDescription(member.getAsMention() + " has been warned **" + warnCount + " times** so far.")
                    .setColor(Constants.YELLOW)
                    .setFooter("ID: " + member.getId())
                    .setTimestamp(new Date().toInstant());
            event.reply(eb.build());
        }
    }
