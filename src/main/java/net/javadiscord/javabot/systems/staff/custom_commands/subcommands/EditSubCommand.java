package net.javadiscord.javabot.systems.staff.custom_commands.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.staff.custom_commands.CustomCommandHandler;
import net.javadiscord.javabot.systems.staff.custom_commands.dao.CustomCommandRepository;
import net.javadiscord.javabot.systems.staff.custom_commands.model.CustomCommand;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Subcommand that allows to edit Custom Slash Commands. {@link CustomCommandHandler#CustomCommandHandler()}
 */
public class EditSubCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var nameOption = event.getOption("name");
		var responseOption = event.getOption("text");
		if (nameOption == null || responseOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var replyOption = event.getOption("reply");
		boolean reply = replyOption == null || replyOption.getAsBoolean();

		var embedOption = event.getOption("embed");
		boolean embed = embedOption == null || embedOption.getAsBoolean();

		String name = CustomCommandHandler.cleanString(nameOption.getAsString());
		String response = responseOption.getAsString();

		var command = new CustomCommand();
		command.setGuildId(event.getGuild().getIdLong());
		command.setCreatedBy(event.getUser().getIdLong());
		command.setName(name);
		command.setResponse(response);
		command.setReply(reply);
		command.setEmbed(embed);

		try (var con = Bot.dataSource.getConnection()) {
			var repo = new CustomCommandRepository(con);
			var c = repo.findByName(event.getGuild().getIdLong(), name);
			if (c.isEmpty()) {
				return Responses.error(event, String.format("A Custom Command called `/%s` does not exist.", name));
			}
			var newCommand = repo.edit(c.get(), command);
			var e = buildEditCommandEmbed(event.getMember(), newCommand);
			Bot.slashCommands.registerSlashCommands(event.getGuild());
			return event.replyEmbeds(e);
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An Error occurred.");
		}
	}

	private MessageEmbed buildEditCommandEmbed(Member createdBy, CustomCommand command) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getUser().getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Custom Command edited")
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
