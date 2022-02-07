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
 * Subcommand that allows to delete Custom Slash Commands. {@link CustomCommandHandler#CustomCommandHandler()}
 */
public class DeleteSubCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var nameOption = event.getOption("name");
		if (nameOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		String name = CustomCommandHandler.cleanString(nameOption.getAsString());

		try (var con = Bot.dataSource.getConnection()) {
			var repo = new CustomCommandRepository(con);
			var command = repo.findByName(event.getGuild().getIdLong(), name);
			if (command.isEmpty()) {
				return Responses.error(event, String.format("Could not find Custom Command with name `/%s`.", name));
			}
			repo.delete(command.get());
			var e = buildDeleteCommandEmbed(event.getMember(), command.get());
			Bot.slashCommands.registerSlashCommands(event.getGuild());
			return event.replyEmbeds(e);
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An Error occurred.");
		}
	}

	private MessageEmbed buildDeleteCommandEmbed(Member deletedBy, CustomCommand command) {
		return new EmbedBuilder()
				.setAuthor(deletedBy.getUser().getAsTag(), null, deletedBy.getEffectiveAvatarUrl())
				.setTitle("Custom Command deleted")
				.addField("Id", String.format("`%s`", command.getId()), true)
				.addField("Name", String.format("`/%s`", command.getName()), true)
				.addField("Created by", deletedBy.getAsMention(), true)
				.addField("Response", String.format("```\n%s\n```", command.getResponse()), false)
				.addField("Reply?", String.format("`%s`", command.isReply()), true)
				.addField("Embed?", String.format("`%s`", command.isEmbed()), true)
				.setColor(Bot.config.get(deletedBy.getGuild()).getSlashCommand().getDefaultColor())
				.setTimestamp(Instant.now())
				.build();
	}
}
