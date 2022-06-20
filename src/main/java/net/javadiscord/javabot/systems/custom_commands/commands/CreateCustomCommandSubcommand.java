package net.javadiscord.javabot.systems.custom_commands.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.custom_commands.model.CustomCommand;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Administrative Subcommand that allows to create {@link CustomCommand}s.
 */
public class CreateCustomCommandSubcommand extends CustomCommandsSubcommand {
	public CreateCustomCommandSubcommand() {
		setSubcommandData(new SubcommandData("create", "Creates a new Custom Commands.")
				.addOption(OptionType.STRING, "name", "The command's name.", true)
				.addOption(OptionType.STRING, "response", "The command's response which should be displayed after execution.", true)
				.addOption(OptionType.BOOLEAN, "reply", "Should the command reply to your message? This default to true.", false)
				.addOption(OptionType.BOOLEAN, "embed", "Should the command be embedded? This defaults to true.", false)
		);
	}

	@Override
	public ReplyCallbackAction handleCustomCommandsSubcommand(@NotNull SlashCommandInteractionEvent event, @NotNull String commandName) throws SQLException {
		OptionMapping responseMapping = event.getOption("response");
		if (responseMapping == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		boolean reply = event.getOption("reply", true, OptionMapping::getAsBoolean);
		boolean embed = event.getOption("embed", true, OptionMapping::getAsBoolean);
		String response = responseMapping.getAsString();

		// build the CustomCommand object
		CustomCommand command = new CustomCommand();
		command.setGuildId(event.getGuild().getIdLong());
		command.setCreatedBy(event.getUser().getIdLong());
		command.setName(commandName);
		command.setResponse(response);
		command.setReply(reply);
		command.setEmbed(embed);
		if (Bot.customCommandManager.addCommand(event.getGuild(), command)) {
			return event.replyEmbeds(buildCreateCommandEmbed(event.getMember(), command));
		}
		return Responses.error(event, "Could not create Custom Command. Please try again.");
	}

	private @NotNull MessageEmbed buildCreateCommandEmbed(@NotNull Member createdBy, @NotNull CustomCommand command) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getUser().getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Custom Command created")
				.addField("Id", String.format("`%s`", command.getId()), true)
				.addField("Name", String.format("`/%s`", command.getName()), true)
				.addField("Created by", createdBy.getAsMention(), true)
				.addField("Response", String.format("```\n%s\n```", command.getResponse()), false)
				.addField("Reply?", String.format("`%s`", command.isReply()), true)
				.addField("Embed?", String.format("`%s`", command.isEmbed()), true)
				.setColor(Bot.config.get(createdBy.getGuild()).getSlashCommand().getDefaultColor())
				.setTimestamp(Instant.now())
				.build();
	}
}
