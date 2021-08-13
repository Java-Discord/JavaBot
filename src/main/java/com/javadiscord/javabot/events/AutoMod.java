package com.javadiscord.javabot.events;

import com.javadiscord.javabot.commands.moderation.Mute;
import com.javadiscord.javabot.commands.moderation.Warn;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoMod extends ListenerAdapter {

    private static final Pattern inviteURL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)\\/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}\\/)\\w+");

    private String cleanString(String input) {
        input = input.replaceAll("\\p{C}", "");
        input = input.replace(" ", "");
        return input;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
            try { if (event.getMember().getUser().isBot() || event.getMember() == null) return; }
            catch (NullPointerException ignored) { return; }

            if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) return;

            Member member = event.getMember();

                if (event.getMessage().getMentionedMembers().size() >= 5) {

                    int warnPoints = new Warn().getWarnCount(member);

                    var eb = new EmbedBuilder()
                            .setColor(Constants.YELLOW)
                            .setAuthor(member.getUser().getAsTag() + " | Warn (" + (warnPoints + 1) + "/3)", null, member.getUser().getEffectiveAvatarUrl())
                            .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                            .addField("Moderator", "```" + event.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                            .addField("ID", "```" + member.getId() + "```", false)
                            .addField("Reason", "```" + "Automod: Mention Spam" + "```", false)
                            .setFooter("ID: " + member.getId())
                            .setTimestamp(new Date().toInstant())
                            .build();

                    event.getChannel().sendMessageEmbeds(eb).queue();
                    Misc.sendToLog(event.getGuild(), eb);
                    member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();

                    try { new Warn().warn(event.getMember(), event.getGuild(), "Automod: Mention Spam"); }
                    catch (Exception e) { event.getChannel().sendMessageEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getAuthor())).queue(); }

                    event.getMessage().delete().complete();
                }

                Matcher matcher = inviteURL.matcher(cleanString(event.getMessage().getContentRaw()));

                if (matcher.find()) {

                    int warnPoints = new Warn().getWarnCount(member);

                    var eb = new EmbedBuilder()
                            .setColor(Constants.YELLOW)
                            .setAuthor(member.getUser().getAsTag() + " | Warn (" + (warnPoints + 1) + "/3)", null, member.getUser().getEffectiveAvatarUrl())
                            .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                            .addField("Moderator", "```" + event.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                            .addField("ID", "```" + member.getId() + "```", false)
                            .addField("Reason", "```" + "Automod: Advertising" + "```", false)
                            .setFooter("ID: " + member.getId())
                            .setTimestamp(new Date().toInstant())
                            .build();

                    event.getChannel().sendMessageEmbeds(eb).queue();
                    Misc.sendToLog(event.getGuild(), eb);
                    member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();

                    try { new Warn().warn(event.getMember(), event.getGuild(), "Automod: Advertising"); }
                    catch (Exception e) { event.getChannel().sendMessageEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getAuthor())).queue(); }

                    event.getMessage().delete().complete();
                }

                List<Message> history = event.getChannel().getIterableHistory().complete().stream()
                        .limit(10).filter(msg -> !msg.equals(event.getMessage())).collect(Collectors.toList());

                int spamCount = history.stream()
                    .filter(message -> message.getAuthor().equals(event.getAuthor()) && !message.getAuthor().isBot())
                    .filter(msg -> (event.getMessage().getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6)
                    .collect(Collectors.toList()).size();


                if (spamCount > 5) {
                    if (!event.getMessage().getAttachments().isEmpty() && event.getMessage().getAttachments().get(0).getFileExtension().equals("java")) return;

                    Role muteRole = event.getGuild().getRoleById(Database.getConfigString(event.getGuild(), "roles.mute_rid"));
                    if (member.getRoles().contains(muteRole)) return;

                    var eb = new EmbedBuilder()
                            .setColor(Constants.RED)
                            .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                            .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                            .addField("Moderator", "```" + event.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                            .addField("ID", "```" + member.getId() + "```", false)
                            .addField("Reason", "```" + "Automod: Spam" + "```", false)
                            .setFooter("ID: " + member.getId())
                            .setTimestamp(new Date().toInstant())
                            .build();

                    event.getChannel().sendMessageEmbeds(eb).queue();
                    Misc.sendToLog(event.getGuild(), eb);
                    member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();

                    try { new Mute().mute(event.getMember(), event.getGuild()); }
                    catch (Exception e) { event.getChannel().sendMessageEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getAuthor())).queue(); }
                }
            }
        }

