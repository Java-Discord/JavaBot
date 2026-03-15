package net.discordjug.javabot.systems.user_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * <h3>This class represents the /id-calc command.</h3>
 */
public class IdCalculatorCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public IdCalculatorCommand() {
		setCommandData(Commands.slash("id-calc", "Generates a human-readable timestamp out of any discord id")
				.addOption(OptionType.STRING, "id", "The ID which should be converted.", true)
				.setContexts(InteractionContextType.GUILD)
		);
	}

	public static long getTimestampFromId(long id) {
		return id / 4194304 + 1420070400000L;
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("id");
		if (idMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (Checks.isInvalidLongInput(idMapping)) {
			Responses.error(event, "Please provide a valid Discord Snowflake!").queue();
			return;
		}
		long id = idMapping.getAsLong();
		event.replyEmbeds(buildIdCalcEmbed(event.getUser(), id, getTimestampFromId(id))).queue();
	}

	private @NotNull MessageEmbed buildIdCalcEmbed(@NotNull User author, long id, long unixTimestamp) {
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(author), null, author.getEffectiveAvatarUrl())
				.setTitle("ID Calculator")
				.setColor(Responses.Type.DEFAULT.getColor())
				.addField("Snowflake Input", String.format(MarkdownUtil.codeblock("%s"), id), false)
				.addField("Unix-Timestamp", String.format(MarkdownUtil.codeblock("%s"), unixTimestamp), true)
				.addField("Unix-Timestamp (+ milliseconds)", String.format(MarkdownUtil.codeblock("%s"), unixTimestamp / 1000), true)
				.addField("Date", String.format("<t:%s:F>", Instant.ofEpochMilli(unixTimestamp).getEpochSecond()), false)
				.build();
	}
}