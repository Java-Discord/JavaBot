package net.javadiscord.javabot.systems.tags.commands;

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
import net.javadiscord.javabot.systems.tags.CustomTagManager;
import net.javadiscord.javabot.systems.tags.dao.CustomTagRepository;
import net.javadiscord.javabot.systems.tags.model.CustomTag;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

/**
 * <h3>This class represents the /tag-admin delete command.</h3>
 */
public class DeleteCustomTagSubcommand extends CustomTagsSubcommand implements AutoCompletable {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public DeleteCustomTagSubcommand() {
		setSubcommandData(new SubcommandData("delete", "Deletes a single Custom Tag.")
				.addOption(OptionType.STRING, "name", "The tag's name.", true, true)
		);
	}

	@Override
	public ReplyCallbackAction handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping nameMapping = event.getOption("name");
		if (nameMapping == null) {
			return Responses.replyMissingArguments(event);
		}
		String tagName = CustomTagManager.cleanString(nameMapping.getAsString());
		DbHelper.doDaoAction(CustomTagRepository::new, dao -> {
			Optional<CustomTag> tagOptional = dao.findByName(event.getGuild().getIdLong(), tagName);
			if (tagOptional.isEmpty()) {
				Responses.error(event.getHook(), "Could not find Custom Tag with name `%s`.", tagName).queue();
				return;
			}
			if (Bot.customTagManager.removeCommand(event.getGuild().getIdLong(), tagOptional.get())) {
				event.getHook().sendMessageEmbeds(buildDeleteCommandEmbed(event.getMember(), tagOptional.get())).queue();
				return;
			}
			Responses.error(event.getHook(), "Could not delete Custom Tag. Please try again.").queue();
		});
		return event.deferReply(true);
	}

	private @NotNull MessageEmbed buildDeleteCommandEmbed(@NotNull Member deletedBy, @NotNull CustomTag command) {
		return new EmbedBuilder()
				.setAuthor(deletedBy.getUser().getAsTag(), null, deletedBy.getEffectiveAvatarUrl())
				.setTitle("Custom Tag Deleted")
				.addField("Id", String.format("`%s`", command.getId()), true)
				.addField("Name", String.format("`%s`", command.getName()), true)
				.addField("Created by", deletedBy.getAsMention(), true)
				.addField("Response", String.format("```\n%s\n```", command.getResponse()), false)
				.addField("Reply?", String.format("`%s`", command.isReply()), true)
				.addField("Embed?", String.format("`%s`", command.isEmbed()), true)
				.setColor(Responses.Type.DEFAULT.getColor())
				.setTimestamp(Instant.now())
				.build();
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		CustomTagManager.handleAutoComplete(event).queue();
	}
}
