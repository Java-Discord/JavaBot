package Commands.Configuation;

import Other.Constants;
import Other.Database;
import Other.Embeds;
import Other.Misc;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.awt.*;

public class WelcomeImage extends Command {

    public WelcomeImage() {
        this.name = "welcomeimage";
    }

    public static void WelcomeImageConfig (CommandEvent event) {

        String guildID = event.getGuild().getId();
        JsonObject welcomeImage = Database.welcomeImage(guildID);
        JsonObject avatarImage = Database.avatarImage(guildID);
        String overlayURL = Database.welcomeImage(event.getGuild().getId()).get("overlayURL").getAsString();

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Welcome Image Configuration")
                .setColor(Constants.GRAY);

            eb.setImage(Misc.checkImage(overlayURL))

            .addField("Image", "Width, Height: ``" + welcomeImage.get("imgW").getAsString() +
                    "``, ``" + welcomeImage.get("imgH").getAsString() +
                    "``\n\n[Overlay](" + overlayURL +
                    "), [Background](" + welcomeImage.get("bgURL").getAsString() + ")", false)

            .addField("Color", "Primary Color: ``#" + Integer.toHexString(welcomeImage.get("primCol").getAsInt()) +
                    "``\nSecondary Color: ``#" + Integer.toHexString(welcomeImage.get("secCol").getAsInt()) + "``", true)

            .addField("Avatar Image", "Width, Height: ``" + avatarImage.get("avW").getAsString() +
                    "``,``" + avatarImage.get("avH").getAsString() +
                    "``\nX, Y: ``" + avatarImage.get("avX").getAsString() +
                    "``, ``" + avatarImage.get("avY").getAsString() + "``", true);
        event.reply(eb.build());

    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

            String[] args = event.getArgs().split("\\s+");

            if (args[0].length() > 1) {

                int i;
                long l;

                switch (args[0].toLowerCase()) {
                    case "list":
                        EmbedBuilder eb = new EmbedBuilder()
                                .setColor(new Color(0x2F3136))
                                .setTitle("WelcomeImage Configuration List")
                                .addField("Available Options", "``list, imgW, imgH, overlayURL, bgURL, primCol, secCol, avX, avY, avH, avW``", false);
                        event.reply(eb.build());

                    break;

                    case "imgw":

                        i = Misc.parseInt(args[1]);
                        Database.queryWelcomeImageInt(event.getGuild().getId(), "imgW", i);
                        event.reply(Embeds.configEmbed(event, "Welcome Image Width", "Welcome Image Width succesfully changed to ", null, String.valueOf(i), true));

                    break;

                    case "imgh":

                        i = Misc.parseInt(args[1]);
                        Database.queryWelcomeImageInt(event.getGuild().getId(), "imgH", i);
                        event.reply(Embeds.configEmbed(event, "Welcome Image Height", "Welcome Image Height succesfully changed to ", null, String.valueOf(i), true));

                    break;

                    case "overlayurl":

                        if (Misc.isImage(args[1])) {
                            Database.queryWelcomeImageString(event.getGuild().getId(), "overlayURL", args[1]);
                            event.reply(Embeds.configEmbed(event, "Welcome Image Overlay", "Welcome Image Overlay succesfully changed to ", Misc.checkImage(args[1]), args[1], true));
                        } else {
                            event.reply(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event));
                        }

                    break;

                    case "bgurl":

                        if (Misc.isImage(args[1])) {
                            Database.queryWelcomeImageString(event.getGuild().getId(), "bgURL", args[1]);
                            event.reply(Embeds.configEmbed(event, "Welcome Image Background", "Welcome Image Background succesfully changed to ", Misc.checkImage(args[1]), args[1], true));
                        } else {
                            event.reply(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event));
                        }

                    break;

                    case "primcol":

                        l = Long.parseLong(args[1], 16);
                        i = (int) l;

                            Database.queryWelcomeImageInt(event.getGuild().getId(), "primCol", i);
                            event.reply(Embeds.configEmbed(event, "Primary Welcome Image Color", "Primary Welcome Image Color succesfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true));

                    break;

                    case "seccol":

                        l = Long.parseLong(args[1], 16);
                        i = (int) l;

                        Database.queryWelcomeImageInt(event.getGuild().getId(), "secCol", i);
                        event.reply(Embeds.configEmbed(event, "Secondary Welcome Image Color", "Secondary Welcome Image Color succesfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true));

                    break;

                    case "avh":

                        i = Misc.parseInt(args[1]);
                        Database.queryAvatarImageInt(event.getGuild().getId(), "avH", i);
                        event.reply(Embeds.configEmbed(event, "Avatar Image Height", "Avatar Image Height succesfully changed to ", null, String.valueOf(i), true));

                    break;

                    case "avw":

                        i = Misc.parseInt(args[1]);
                        Database.queryAvatarImageInt(event.getGuild().getId(), "avW", i);
                        event.reply(Embeds.configEmbed(event, "Avatar Image Width", "Avatar Image Width succesfully changed to ", null, String.valueOf(i), true));

                    break;

                    case "avx":

                        i = Misc.parseInt(args[1]);
                        Database.queryAvatarImageInt(event.getGuild().getId(), "avX", i);
                        event.reply(Embeds.configEmbed(event, "Avatar Image (X-Pos)", "Avatar Image ``(X-Position)`` succesfully changed to ", null, String.valueOf(i), true));

                    break;

                    case "avy":

                        i = Misc.parseInt(args[1]);
                        Database.queryAvatarImageInt(event.getGuild().getId(), "avY", i);
                        event.reply(Embeds.configEmbed(event, "Avatar Image (Y-Pos)", "Avatar Image ``(Y-Position)`` succesfully changed to ", null, String.valueOf(i), true));

                    break;

                    default:
                        event.reply(Embeds.emptyError("```Option not available.```", event));
                }

            } else { WelcomeImageConfig(event); }
            } else { event.reply(Embeds.permissionError("ADMINISTRATOR", event)); }

        }
    }
