package Commands.Moderation;

import Other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.apache.logging.log4j.core.util.ArrayUtils;

public class EditEmbed extends Command {

    public EditEmbed () {
        this.name = "editembed";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            try {
                String mID = event.getMessage().getContentRaw().split("\\s+")[1];
                String[] embedargs = event.getMessage().getContentRaw().split("\\s+");

                for (int i = 1; i > -1; i--) { embedargs = ArrayUtils.remove(embedargs, i); }

                StringBuffer sb = new StringBuffer();
                for(int i = 0; i < embedargs.length; i++) { sb.append(embedargs[i] + " "); }

                String par = sb.toString();

                String title = par.split("~;")[0];
                String description = par.split("~;")[1];

                Message message = event.getChannel().retrieveMessageById(mID).complete();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(message.getEmbeds().get(0).getColor())
                        .setTitle(title)
                        .setDescription(description);

                message.editMessage(eb.build()).queue();

            } catch (IndexOutOfBoundsException e) {
                event.reply(Embeds.syntaxError("editembed MessageID Title~;Description", event)); }

        } else { event.reply(Embeds.permissionError("MESSAGE_MANAGE", event)); }
    }
}