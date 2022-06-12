package net.javadiscord.javabot.systems.staff.custom_commands.subcommands;

import net.javadiscord.javabot.systems.staff.custom_commands.CustomCommandHandler;

/**
 * Subcommand that allows to create Custom Slash Commands. {@link CustomCommandHandler#CustomCommandHandler()}
 */
// TODO: Re-implement Custom Commands
public class CreateSubcommand
//		extends SlashCommand.Subcommand
{
//	@Override
//	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
//		var nameOption = event.getOption("name");
//		var responseOption = event.getOption("text");
//		if (nameOption == null || responseOption == null) {
//			return Responses.error(event, "Missing required arguments.");
//		}
//		var replyOption = event.getOption("reply");
//		boolean reply = replyOption == null || replyOption.getAsBoolean();
//
//		var embedOption = event.getOption("embed");
//		boolean embed = embedOption == null || embedOption.getAsBoolean();
//
//		String name = CustomCommandHandler.cleanString(nameOption.getAsString());
//		String response = responseOption.getAsString();
//
//		var command = new CustomCommand();
//		command.setGuildId(event.getGuild().getIdLong());
//		command.setCreatedBy(event.getUser().getIdLong());
//		command.setName(name);
//		command.setResponse(response);
//		command.setReply(reply);
//		command.setEmbed(embed);
//
//		if(Bot.interactionHandler.doesSlashCommandExist(name, event.getGuild())){
//			return Responses.error(event, "This command already exists.");
//		}
//
//		try (var con = Bot.dataSource.getConnection()) {
//			var c = new CustomCommandRepository(con).insert(command);
//			var e = buildCreateCommandEmbed(event.getMember(), c);
//			Bot.interactionHandler.registerCommands(event.getGuild());
//			return event.replyEmbeds(e);
//		} catch (SQLException e) {
//			e.printStackTrace();
//			return Responses.error(event, "An Error occurred.");
//		}
//	}
//
//	private MessageEmbed buildCreateCommandEmbed(Member createdBy, CustomCommand command) {
//		return new EmbedBuilder()
//				.setAuthor(createdBy.getUser().getAsTag(), null, createdBy.getEffectiveAvatarUrl())
//				.setTitle("Custom Command created")
//				.addField("Id", String.format("`%s`", command.getId()), true)
//				.addField("Name", String.format("`/%s`", command.getName()), true)
//				.addField("Created by", createdBy.getAsMention(), true)
//				.addField("Response", String.format("```\n%s\n```", command.getResponse()), false)
//				.addField("Reply?", String.format("`%s`", command.isReply()), true)
//				.addField("Embed?", String.format("`%s`", command.isEmbed()), true)
//				.setColor(Bot.config.get(createdBy.getGuild()).getSlashCommand().getDefaultColor())
//				.setTimestamp(Instant.now())
//				.build();
//	}
}
