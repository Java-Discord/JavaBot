package net.javadiscord.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /warn add command.</h3>
 * This Subcommand allows staff-members to add a single warn to any user.
 */
public class WarnExportSubcommand extends SlashCommand.Subcommand {
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param warnRepository DAO for interacting with the set of {@link net.javadiscord.javabot.systems.moderation.warn.model.Warn} objects.
	 */
	public WarnExportSubcommand(NotificationService notificationService, BotConfig botConfig, ExecutorService asyncPool, WarnRepository warnRepository) {
		this.notificationService = notificationService;
		this.botConfig = botConfig;
		this.warnRepository = warnRepository;
		this.asyncPool = asyncPool;
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
		User target = userMapping.getAsUser();
		ModerationService service = new ModerationService(notificationService, botConfig, event, warnRepository, asyncPool);
		List<Warn> warns = service.getAllWarns(target.getIdLong());
		PipedInputStream pis=new PipedInputStream();
		try(PrintWriter pw=new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new PipedOutputStream(pis)), StandardCharsets.UTF_8))){
			event.replyEmbeds(new EmbedBuilder()
					.setAuthor(target.getAsTag(), null, target.getAvatarUrl())
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

