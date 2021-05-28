package Commands.UserCommands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class IDCalc extends Command {

    public static void exCommand (CommandEvent event) {

        long Input;
        String[] args = event.getArgs().split("\\s+");

        try {
            if (!args[0].isEmpty()) {
                Input = Long.parseLong(args[0]);
            } else {
                Input = event.getAuthor().getIdLong();
            }

        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            Input = event.getAuthor().getIdLong();
        }

        long unixTimeStampMilliseconds = Input / 4194304 + 1420070400000L;
        long unixTimeStamp = unixTimeStampMilliseconds / 1000;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE',' dd/MM/yyyy',' HH:mm", new Locale("en"));
        String Date = Instant.ofEpochMilli(unixTimeStampMilliseconds).atZone(ZoneId.of("GMT")).format(dtf);

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("ID-Calculator")
                .setColor(new Color(0x2F3136))
                .addField("ID", "```" + Input + "```", false)
                .addField("Unix-Timestamp (+ milliseconds)", "```" + unixTimeStampMilliseconds + "```", false)
                .addField("Unix-Timestamp", "```" + unixTimeStamp + "```", false)
                .addField("Date", "```" + Date + "```", false);

        event.reply(eb.build());
    }

    public static void exCommand (SlashCommandEvent event, long id) {

        long unixTimeStampMilliseconds = id / 4194304 + 1420070400000L;
        long unixTimeStamp = unixTimeStampMilliseconds / 1000;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE',' dd/MM/yyyy',' HH:mm", new Locale("en"));
        String Date = Instant.ofEpochMilli(unixTimeStampMilliseconds).atZone(ZoneId.of("GMT")).format(dtf);

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("ID-Calculator")
                .setColor(new Color(0x2F3136))
                .addField("ID", "```" + id + "```", false)
                .addField("Unix-Timestamp (+ milliseconds)", "```" + unixTimeStampMilliseconds + "```", false)
                .addField("Unix-Timestamp", "```" + unixTimeStamp + "```", false)
                .addField("Date", "```" + Date + "```", false);

        event.replyEmbeds(eb.build()).queue();
    }

    public IDCalc() {
        this.name = "idcalc";
        this.category = new Category("USER COMMANDS");
        this.arguments = "<ID>";
        this.help = "Generates a human-readable timestamp out of a given id";
    }

    @Override
    protected void execute(CommandEvent event) {

        exCommand(event);
    }
}