package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;

import java.time.Instant;

/**
 * Command that allows users to convert discord ids into a human-readable timestamp.
 */
public class IdCalculatorCommand extends SlashCommand {
	public IdCalculatorCommand() {
		setCommandData(Commands.slash("id-calc", "Generates a human-readable timestamp out of any discord id")
				.addOption(OptionType.STRING, "snowflake", "The ID which should be converted.", true));
	}

	public static long getTimestampFromId(long id) {
		return id / 4194304 + 1420070400000L;
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping idOption = event.getOption("id");
		if (idOption == null) {
			Responses.error(event, "Missing required arguments").queue();
			return;
		}
		if (!Checks.checkLongInput(idOption)) {
			Responses.error(event, "Please provide a valid Discord ID!").queue();
			return;
		}
		long id = idOption.getAsLong();
		SlashCommandConfig config = Bot.config.get(event.getGuild()).getSlashCommand();
		event.replyEmbeds(buildIdCalcEmbed(event.getUser(), id, IdCalculatorCommand.getTimestampFromId(id), config)).queue();
	}

	private MessageEmbed buildIdCalcEmbed(User author, long id, long unixTimestamp, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("ID-Calculator")
				.setColor(config.getDefaultColor())
				.addField("Input", String.format("`%s`", id), false)
				.addField("Unix-Timestamp", String.format("`%s`", unixTimestamp), true)
				.addField("Unix-Timestamp (+ milliseconds)", String.format("`%s`", unixTimestamp / 1000), true)
				.addField("Date", String.format("<t:%s:F>", Instant.ofEpochMilli(unixTimestamp / 1000).getEpochSecond()), false)
				.build();
	}
}