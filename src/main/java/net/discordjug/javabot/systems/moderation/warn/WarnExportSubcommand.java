package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.systems.moderation.warn.model.Warn;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /warn add command.</h3>
 * This Subcommand allows staff-members to add a single warn to any user.
 */
public class WarnExportSubcommand extends SlashCommand.Subcommand {
	private final BotConfig botConfig;
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public WarnExportSubcommand(BotConfig botConfig, ModerationService moderationService) {
		this.botConfig = botConfig;
		this.moderationService = moderationService;
		setCommandData(new SubcommandData("export", "Exports a list of all warns of a user")
				.addOptions(
						new OptionData(OptionType.USER, "user", "The user to warn.", true)
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping userMapping = event.getOption("user");
		if (userMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if(!Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.replyStaffOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		User target = userMapping.getAsUser();
		List<Warn> warns = moderationService.getAllWarns(target.getIdLong());
		PipedInputStream pis=new PipedInputStream();
		try(PrintWriter pw=new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new PipedOutputStream(pis)), StandardCharsets.UTF_8))){
			event.replyEmbeds(new EmbedBuilder()
					.setAuthor(UserUtils.getUserTag(target), null, target.getAvatarUrl())
					.setDescription("Export containing all warns of "+target.getAsMention())
					.addField("Total number of warns", String.valueOf(warns.stream().count()), false)
					.addField("Total number of non-discarded warns (includes expired warns)", String.valueOf(warns.stream().filter(w->!w.isDiscarded()).count()), false)
					.addField("Total severity of non-discarded warns (includes expired warns)", String.valueOf(warns.stream().filter(w->!w.isDiscarded()).mapToInt(Warn::getSeverityWeight).sum()), false)
					.setFooter(target.getId())
					.build())
				.addFiles(FileUpload.fromData(pis, "warns"+target.getId()+".txt"))
			.queue();
			for (Iterator<Warn> it = warns.iterator(); it.hasNext();) {
				Warn warn = it.next();
				pw.println("Reason: \"" + warn.getReason()+"\"");
				pw.println("Severity: " + warn.getSeverity() + "(" + warn.getSeverityWeight() + ")");
				pw.println("Warn ID: " + warn.getId());
				pw.println("Warned by: " + warn.getWarnedBy());
				pw.println("Warned at " + warn.getCreatedAt());
				if (warn.isDiscarded()) {
					pw.print("This warn has been discarded.");
				}
				if (it.hasNext()) {
					pw.println();
					pw.println("============");
					pw.println();
				}
			}
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
	}
}

