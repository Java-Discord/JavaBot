package net.javadiscord.javabot.systems.custom_commands.commands;

import com.dynxsty.dih4jda.interactions.commands.AutoCompletable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
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

import java.time.Instant;
import java.util.Optional;

/**
 * <h3>This class represents the /customcommand-admin delete command.</h3>
 */
public class DeleteCustomCommandSubcommand extends CustomCommandsSubcommand implements AutoCompletable {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public DeleteCustomCommandSubcommand() {
		setSubcommandData(new SubcommandData("delete", "Deletes a single Custom Command.")
				.addOption(OptionType.STRING, "name", "The command's name.", true, true)
		);
	}

	@Override
	public ReplyCallbackAction handleCustomCommandsSubcommand(@NotNull SlashCommandInteractionEvent event, @NotNull String commandName) {
		DbHelper.doDaoAction(CustomCommandRepository::new, dao -> {
			Optional<CustomCommand> commandOptional = dao.findByName(event.getGuild().getIdLong(), commandName);
			if (commandOptional.isEmpty()) {
				Responses.error(event.getHook(), String.format("Could not find Custom Command with name `/%s`.", commandOptional)).queue();
				return;
			}
			if (Bot.customCommandManager.removeCommand(event.getGuild(), commandOptional.get())) {
				event.getHook().sendMessageEmbeds(buildDeleteCommandEmbed(event.getMember(), commandOptional.get())).queue();
				return;
			}
			Responses.error(event.getHook(), "Could not delete Custom Command. Please try again.").queue();
		});
		return event.deferReply(true);
	}

	private @NotNull MessageEmbed buildDeleteCommandEmbed(@NotNull Member deletedBy, @NotNull CustomCommand command) {
		return new EmbedBuilder()
				.setAuthor(deletedBy.getUser().getAsTag(), null, deletedBy.getEffectiveAvatarUrl())
				.setTitle("Custom Command deleted")
				.addField("Id", String.format("`%s`", command.getId()), true)
				.addField("Name", String.format("`/%s`", command.getName()), true)
				.addField("Created by", deletedBy.getAsMention(), true)
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
