package Other;

import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class StatsCategory {

    public static void update (Object ev) {

        Guild guild = null;

        if (ev instanceof com.jagrosh.jdautilities.command.CommandEvent) {
            com.jagrosh.jdautilities.command.CommandEvent event = (CommandEvent) ev;

            guild = event.getGuild();
        }

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            guild = event.getGuild();
        }

        if (ev instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent event = (GuildMemberJoinEvent) ev;

            guild = event.getGuild();
        }

        Object event = ev;

        String text = Database.getConfigString(event, "stats_msg")
                .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                .replace("{!server}", guild.getName());

        guild.getCategoryById(Database.getConfigString(event, "stats_cid")).getManager().setName(text).queue();
    }
}
