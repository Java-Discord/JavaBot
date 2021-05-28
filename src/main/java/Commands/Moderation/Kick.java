package Commands.Moderation;

import Other.Constants;
import Other.Embeds;
import Other.Misc;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.awt.*;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Kick extends Command {

    public void kick(Member member, String reason, String moderatorTag, CommandEvent event){

        try {

            member.kick(reason).queueAfter(3, TimeUnit.SECONDS);

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag() + " | Kick", null, member.getUser().getEffectiveAvatarUrl())
                    .setColor(Constants.RED)
                    .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                    .addField("Moderator", "```" + moderatorTag + "```", true)
                    .addField("ID", "```" + member.getId() + "```", false)
                    .addField("Reason", "```" + reason + "```", false)
                    .setFooter("ID: " + member.getId())
                    .setTimestamp(new Date().toInstant());

            Misc.sendToLog(event, eb.build());
            member.getUser().openPrivateChannel().complete().sendMessage(eb.build()).queue();
            event.reply(eb.build());

        } catch (HierarchyException e) {
            event.reply(Embeds.hierarchyError(event));

        } catch (NullPointerException | NumberFormatException e) {
            event.reply(Embeds.syntaxError("kick @User/ID (Reason)", event));
        }
    }

    public Kick () { this.name = "kick"; }

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
                        event.reply(Embeds.selfPunishError("kick", event));
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

                kick(member, reason, event.getAuthor().getAsTag(), event);

            } catch (NullPointerException | IllegalArgumentException e) { event.reply(Embeds.syntaxError("kick @User/ID (Reason)", event)); }
            } else { event.reply(Embeds.permissionError("KICK_MEMBERS", event)); }
    }
}