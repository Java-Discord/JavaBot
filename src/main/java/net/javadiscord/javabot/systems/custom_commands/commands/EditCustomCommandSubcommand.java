package net.javadiscord.javabot.systems.custom_commands.commands;

import com.dynxsty.dih4jda.interactions.commands.AutoCompletable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.custom_commands.CustomCommandManager;
import net.javadiscord.javabot.systems.custom_commands.dao.CustomCommandRepository;
import net.javadiscord.javabot.systems.custom_commands.model.CustomCommand;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * <h3>This class represents the /customcommands-admin command.</h3>
 */
public class EditCustomCommandSubcommand extends CustomCommandsSubcommand implements AutoCompletable {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public EditCustomCommandSubcommand() {
		setSubcommandData(new SubcommandData("edit", "Edits a single Custom Commands.")
				.addOption(OptionType.STRING, "name", "The command's name.", true, true)
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
		CustomCommand update = new CustomCommand();
		update.setGuildId(event.getGuild().getIdLong());
		update.setCreatedBy(event.getUser().getIdLong());
		update.setName(commandName);
		update.setResponse(response);
		update.setReply(reply);
		update.setEmbed(embed);

		DbHelper.doDaoAction(CustomCommandRepository::new, dao -> {
			Optional<CustomCommand> commandOptional = dao.findByName(event.getGuild().getIdLong(), commandName);
			if (commandOptional.isEmpty()) {
				Responses.error(event.getHook(), String.format("Could not find Custom Command with name `/%s`.", commandOptional)).queue();
				return;
			}
			if (Bot.customCommandManager.editCommand(event.getGuild(), commandOptional.get(), update)) {
				event.getHook().sendMessageEmbeds(buildEditCommandEmbed(event.getMember(), update)).queue();
				return;
			}
			Responses.error(event.getHook(), "Could not edit Custom Command. Please try again.").queue();
		});
		return event.deferReply(true);
	}

	private @NotNull MessageEmbed buildEditCommandEmbed(@NotNull Member createdBy, @NotNull CustomCommand command) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getUser().getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Custom Command edited")
				.addField("Id", String.format("`%s`", command.getId()), true)
				.addField("Name", String.format("`/%s`", command.getName()), true)
				.addField("Created by", createdBy.getAsMention(), true)
				.addField("Response", String.format("```\n%s\n```", command.getResponse()), false)
				.addField("Reply?", String.format("`%s`", command.isReply()), true)
				.addField("Embed?", String.format("`%s`", command.isEmbed()), true)
				.setColor(Responses.Type.DEFAULT.getColor())
				.setTimestamp(Instant.now())
				.build();
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		event.replyChoices(CustomCommandManager.replyCustomCommands()).queue();
	}
}
