package Commands.Moderation;

import Other.Constants;
import Other.Database;
import Other.Embeds;
import Other.Misc;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.awt.*;
import java.util.Date;

public class Unmute extends Command {

    public Unmute () { this.name = "unmute"; }

    protected void execute(CommandEvent event) {
            if (event.getMember().hasPermission(Permission.MANAGE_ROLES)) {

                String[] args = event.getArgs().split("\\s+");
                Member member = null;

                try {

                    if (args.length >= 1) {
                        if (!event.getMessage().getMentionedMembers().isEmpty()) {
                            member = event.getMessage().getMentionedMembers().get(0);

                        } else {
                            member = event.getGuild().getMemberById(args[0]);
                        }

                        if (event.getMessage().getMember().equals(member)) {
                            event.reply(Embeds.selfPunishError("unmute", event));
                            return;
                        }
                    }

                    Role muteRole = Database.configRole(event, "mute_rid");

                    try {
                        event.getGuild().removeRoleFromMember(member.getId(), muteRole).complete();

                        EmbedBuilder eb = new EmbedBuilder()
                                .setAuthor(member.getUser().getAsTag() + " | Unmute", null, member.getUser().getEffectiveAvatarUrl())
                                .setColor(Constants.RED)
                                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                                .addField("Moderator", "```" + event.getMessage().getAuthor().getAsTag() + "```", true)
                                .addField("ID", "```" + member.getId() + "```", false)
                                .setFooter("ID: " + member.getId())
                                .setTimestamp(new Date().toInstant());

                        member.getUser().openPrivateChannel().complete().sendMessage(eb.build()).queue();
                        Misc.sendToLog(event, eb.build());
                        event.reply(eb.build());

                    } catch (HierarchyException e) { event.reply(Embeds.hierarchyError(event)); }
                    } catch (NullPointerException | IllegalArgumentException e) { event.reply(Embeds.syntaxError("unmute @User/ID", event)); }
                    } else { event.reply(Embeds.permissionError("MANAGE_ROLES", event)); }
        }
    }
