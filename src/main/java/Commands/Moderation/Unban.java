package Commands.Moderation;

import Other.Embeds;
import Other.Misc;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.awt.*;
import java.util.Date;

public class Unban extends Command {

    public Unban () {
        this.name = "unban";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.BAN_MEMBERS)) {

            Member member = event.getMember();
            String[] args = event.getArgs().split("\\s+");

            try {

                event.getGuild().unban(args[0]).complete();

                EmbedBuilder eb = new EmbedBuilder()
                        .setAuthor(member.getUser().getAsTag() + " | Unban", null, member.getUser().getEffectiveAvatarUrl())
                        .setColor(Color.RED)
                        .addField("ID", "```" + member.getId() + "```", true)
                        .addField("Moderator", "```" + event.getAuthor().getAsTag() + "```", true)
                        .setFooter("ID: " + member.getId())
                        .setTimestamp(new Date().toInstant());
                event.reply(eb.build());
                Misc.sendToLog(event, eb.build());

            } catch (IllegalArgumentException e) {
                event.reply(Embeds.syntaxError("unban @User/ID", event));

            } catch (ErrorResponseException e) {
                event.reply(Embeds.emptyError("```User (" + args[0] + ") not found.```", event));
            }

            } else {
            event.reply(Embeds.permissionError("BAN_MEMBERS", event));
        }
    }
}

