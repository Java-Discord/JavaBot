package Commands.Configuation;

import Other.Constants;
import Other.Database;
import Other.Embeds;
import Other.Misc;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.awt.*;
import java.util.Arrays;

public class Config extends Command {

    public Config () { this.name = "config"; }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

            String[] args = event.getArgs().split("\\s+");

            if (args[0].length() > 0) {

                Message message = event.getMessage();
                String param;
                String[] configArg;
                StringBuilder builder;
                String text;


                switch (args[0]) {
                    case "list":
                        EmbedBuilder eb = new EmbedBuilder()
                                .setColor(new Color(0x2F3136))
                                .setTitle("Bot Configuration List")
                                .addField("Available Options", "``list, leavemsg, welcomemsg, welcomechannel, statscategory, statsmsg, reportchannel, logchannel, muterole, suggestchannel, submissionchannel, dm-qotw, lock``", false)
                                .addField("Extra Arguments", "```{!member}, {!membertag}, {!server}```", false);

                        MessageChannel channel = event.getChannel();
                        channel.sendMessage(eb.build())
                                .queue();
                        break;

                    case "leavemsg":

                        configArg = Arrays.copyOfRange(args, 1, args.length);
                        builder = new StringBuilder();
                        for (String value : configArg) {
                            builder.append(value + " ");
                        }
                        text = builder.substring(0, builder.toString().length() - 1);

                        Database.queryConfigString(event.getGuild().getId(), "leave_msg", text);
                        event.reply(Embeds.configEmbed(event, "Leave Message", "Leave Message succesfully changed to", null, text, true));
                        break;

                    case "welcomemsg":

                        configArg = Arrays.copyOfRange(args, 1, args.length);
                        builder = new StringBuilder();
                        for (String value : configArg) {
                            builder.append(value + " ");
                        }
                        text = builder.substring(0, builder.toString().length() - 1);

                        Database.queryConfigString(event.getGuild().getId(), "welcome_msg", text);
                        event.reply(Embeds.configEmbed(event, "Welcome Message", "Welcome Message succesfully changed to", null, text, true));
                        break;

                    case "welcomechannel":

                        if(!message.getMentionedChannels().isEmpty()) {
                            param = message.getMentionedChannels().get(0).getId();
                        } else {
                            param = args[1];
                        }

                        Database.queryConfigString(event.getGuild().getId(), "welcome_cid", param);
                        event.reply(Embeds.configEmbed(event, "Welcome Channel", "Welcome Channel succesfully changed to", null, param, true, true));
                        break;

                    case "statscategory":
                        Database.queryConfigString(event.getGuild().getId(), "stats_cid", args[1]);
                        event.reply(Embeds.configEmbed(event, "Stats-Category ID", "Stats-Category ID succesfully changed to", null, args[1], true));
                        break;

                    case "statsmsg":

                        configArg = Arrays.copyOfRange(args, 1, args.length);
                        builder = new StringBuilder();
                        for (String value : configArg) {
                            builder.append(value + " ");
                        }
                        text = builder.substring(0, builder.toString().length() - 1);

                        Database.queryConfigString(event.getGuild().getId(), "stats_msg", text);
                        event.reply(Embeds.configEmbed(event, "Stats-Category Message", "Stats-Category Message succesfully changed to", null, text, true));
                        break;

                    case "reportchannel":

                        if(!message.getMentionedChannels().isEmpty()) {
                            param = message.getMentionedChannels().get(0).getId();
                        } else {
                            param = args[1];
                        }

                        Database.queryConfigString(event.getGuild().getId(), "report_cid", param);
                        event.reply(Embeds.configEmbed(event, "Report Channel", "Report Channel succesfully changed to", null, param, true, true));
                        break;

                    case "logchannel":

                        if(!message.getMentionedChannels().isEmpty()) {
                            param = message.getMentionedChannels().get(0).getId();
                        } else {
                            param = args[1];
                        }

                        Database.queryConfigString(event.getGuild().getId(), "log_cid", param);
                        event.reply(Embeds.configEmbed(event, "Log Channel", "Log Channel succesfully changed to", null, param, true, true));
                        break;

                    case "muterole":

                        if(!message.getMentionedRoles().isEmpty()) {
                            param = message.getMentionedRoles().get(0).getId();
                        } else {
                            param = args[1];
                        }

                        Database.queryConfigString(event.getGuild().getId(), "mute_rid", param);
                        event.reply(Embeds.configEmbed(event, "Mute Role", "Mute Role succesfully changed to", null, param, true, false, true));
                        break;

                    case "suggestchannel":

                        if(!message.getMentionedChannels().isEmpty()) {
                            param = message.getMentionedChannels().get(0).getId();
                        } else {
                            param = args[1];
                        }

                        Database.queryConfigString(event.getGuild().getId(), "suggestion_cid", param);
                        event.reply(Embeds.configEmbed(event, "Suggest Channel", "Suggest Channel succesfully changed to", null, param, true, true));
                        break;

                    case "submissionchannel":

                        if(!message.getMentionedChannels().isEmpty()) {
                            param = message.getMentionedChannels().get(0).getId();
                        } else {
                            param = args[1];
                        }

                        Database.queryConfigString(event.getGuild().getId(), "submission_cid", param);
                        event.reply(Embeds.configEmbed(event, "QOTW-Submission Channel", "QOTW-Submission Channel succesfully changed to", null, param, true, true));
                        break;

                    case "dm-qotw":
                        String qotwstatus = Database.getConfigString(event, "dm-qotw");

                        if(qotwstatus.equalsIgnoreCase("true")) {
                            qotwstatus = "false";
                        } else {
                            qotwstatus = "true";
                        }

                        Database.queryConfigString(event.getGuild().getId(), "dm-qotw", qotwstatus);
                        event.reply(Embeds.configEmbed(event, "QOTW-DM Status", "QOTW-DM Status succesfully changed to", null, qotwstatus, true));
                        break;

                    case "lock":
                        String lockstatus = Database.getConfigString(event, "lock");
                        Database.queryConfigInt(event.getGuild().getId(), "lockcount", 0);
                        String title, desc;

                        if(lockstatus.equalsIgnoreCase("true")) {
                            lockstatus = "false";
                            title = "Server unlocked";
                            desc = "Server succesfully unlocked! \uD83D\uDD13";
                        } else {
                            lockstatus = "true";
                            title = "Server locked";
                            desc = "Server succesfully locked! \uD83D\uDD12";
                        }

                        Database.queryConfigString(event.getGuild().getId(), "lock", lockstatus);
                        event.reply(Embeds.configEmbed(event, title, desc, null, lockstatus));
                        break;

                    default:
                        event.reply(Embeds.emptyError("```Option not available.```", event));
                }

            } else {

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Constants.GRAY)
                        .setTitle("Bot Configuration");

                String overlayURL = Database.welcomeImage(event.getGuild().getId()).get("overlayURL").getAsString();

                eb.setImage(Misc.checkImage(overlayURL));

                        eb.addField("Server locked?", "``" + Database.getConfigString(event, "lock") + ", " + Database.getConfigString(event, "lockcount") + "``", false)
                        .addField("QOTW", "<#" + Database.getConfigString(event, "submission_cid") + ">\n" + "DM-Submissions: ``" + Database.getConfigString(event, "dm-qotw") + "``", false)
                        .addField("Stats-Category", Database.getConfigString(event, "stats_cid") + "\n``" + Database.getConfigString(event, "stats_msg") + "``", true)
                        .addField("Report", "<#" + Database.getConfigString(event, "report_cid") + ">", true)
                        .addField("Log", "<#" + Database.getConfigString(event, "log_cid") + ">", true)
                        .addField("Mute", "<@&" + Database.getConfigString(event, "mute_rid") + ">", true)
                        .addField("Suggestions", "<#" + Database.getConfigString(event, "suggestion_cid") + ">", true)
                        .addField("Welcome-System", "<#" + Database.getConfigString(event, "welcome_cid") +
                                ">\nWelcome Message: ``" + Database.getConfigString(event, "welcome_msg") +
                                "``\nLeave Message: ``" + Database.getConfigString(event, "leave_msg") +
                                "``\n[Image Link](" + overlayURL + ")", false);

                event.reply(eb.build());
            }

        } else { event.reply(Embeds.permissionError("ADMINISTRATOR", event)); }

    }
}
