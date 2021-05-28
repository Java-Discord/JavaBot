package Commands.Other.Testing;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;
import java.util.Date;

public class SampleSuggestion extends Command {

    public SampleSuggestion () {
        this.name = "samplesuggestion";
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.help = "sends a sample suggestion";
    }

    protected void execute(CommandEvent event) {

        Member member = event.getMember();

        Emote Upvote = event.getGuild().getEmotesByName("up_vote", false).get(0);
        Emote Downvote = event.getGuild().getEmotesByName("down_vote", false).get(0);

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " · Suggestion", null, member.getUser().getEffectiveAvatarUrl())
                .setDescription("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labor")
                .setTimestamp(new Date().toInstant())
                .setColor(new Color(0x2F3136));

        event.getChannel().sendMessage(eb.build()).queue(message -> {
            message.addReaction(Upvote).queue();
            message.addReaction(Downvote).queue();
        });
    }
}