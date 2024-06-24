package net.discordjug.javabot.systems.staff_commands.tags.commands;

import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.tags.CustomTagManager;
import net.discordjug.javabot.systems.staff_commands.tags.model.CustomTag;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.RestAction;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * <h3>This class represents the /tag view command.</h3>
 */
public class TagViewSubcommand extends TagsSubcommand implements AutoCompletable {
	private final CustomTagManager tagManager;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param tagManager The {@link CustomTagManager}
	 * @param botConfig The main configuration of the bot
	 */
	public TagViewSubcommand(CustomTagManager tagManager, BotConfig botConfig) {
		super(botConfig);
		this.tagManager = tagManager;
		setCommandData(new SubcommandData("view", "Allows to view a tag.")
				.addOption(OptionType.STRING, "name", "The tag's name.", true, true)
		);
		setRequiredStaff(false);
	}

	@Override
	public RestAction<?> handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping nameMapping = event.getOption("name");
		if (nameMapping == null) {
			return Responses.replyMissingArguments(event);
		}
		Optional<CustomTag> tagOptional = tagManager.getByName(event.getGuild().getIdLong(), nameMapping.getAsString());
		if (tagOptional.isPresent()) {
			return CustomTagManager.handleCustomTag(event, tagOptional.get());
		} else {
			return Responses.error(event, "Could not find Custom Tag with name `%s`.", nameMapping.getAsString());
		}
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		CustomTagManager.handleAutoComplete(event).queue();
	}
}
