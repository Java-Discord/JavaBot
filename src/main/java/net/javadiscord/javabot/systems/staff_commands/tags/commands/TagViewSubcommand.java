package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import com.dynxsty.dih4jda.interactions.commands.AutoCompletable;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.staff_commands.tags.CustomTagManager;
import net.javadiscord.javabot.systems.staff_commands.tags.model.CustomTag;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * <h3>This class represents the /tag command.</h3>
 */
public class TagViewSubcommand extends TagsSubcommand implements AutoCompletable {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public TagViewSubcommand() {
		setSubcommandData(new SubcommandData("view", "Allows to view a tag.")
				.addOption(OptionType.STRING, "name", "The tag's name.", true, true)
		);
	}

	@Override
	public ReplyCallbackAction handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping nameMapping = event.getOption("name");
		if (nameMapping == null) {
			return Responses.replyMissingArguments(event);
		}
		Optional<CustomTag> tagOptional = Bot.customTagManager.getByName(event.getGuild().getIdLong(), nameMapping.getAsString());
		if (tagOptional.isPresent()) {
			CustomTagManager.handleCustomTag(event, tagOptional.get()).queue();
		} else {
			Responses.error(event.getHook(), "Could not find Custom Tag with name `%s`.", nameMapping.getAsString());
		}
		return event.deferReply(false);
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		CustomTagManager.handleAutoComplete(event).queue();
	}
}
