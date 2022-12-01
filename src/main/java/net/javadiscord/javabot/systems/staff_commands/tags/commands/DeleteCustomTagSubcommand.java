package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
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
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.staff_commands.tags.CustomTagManager;
import net.javadiscord.javabot.systems.staff_commands.tags.dao.CustomTagRepository;
import net.javadiscord.javabot.systems.staff_commands.tags.model.CustomTag;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * <h3>This class represents the /tag-admin delete command.</h3>
 */
public class DeleteCustomTagSubcommand extends TagsSubcommand implements AutoCompletable {
	private final CustomTagManager customTagManager;
	private final ExecutorService asyncPool;
	private final CustomTagRepository customTagRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param customTagManager The {@link CustomTagManager}
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param customTagRepository Dao object that represents the CUSTOM_COMMANDS SQL Table.
	 */
	public DeleteCustomTagSubcommand(CustomTagManager customTagManager, BotConfig botConfig, ExecutorService asyncPool, CustomTagRepository customTagRepository) {
		super(botConfig);
		this.customTagManager = customTagManager;
		this.asyncPool = asyncPool;
		this.customTagRepository = customTagRepository;
		setCommandData(new SubcommandData("delete", "Deletes a single Custom Tag.")
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
		asyncPool.execute(()->{
			try {
				Optional<CustomTag> tagOptional = customTagRepository.findByName(event.getGuild().getIdLong(), tagName);
				if (tagOptional.isEmpty()) {
					Responses.error(event.getHook(), "Could not find Custom Tag with name `%s`.", tagName).queue();
					return;
				}
				if (customTagManager.removeCommand(event.getGuild().getIdLong(), tagOptional.get())) {
					event.getHook().sendMessageEmbeds(buildDeleteCommandEmbed(event.getMember(), tagOptional.get())).queue();
					return;
				}
				Responses.error(event.getHook(), "Could not delete Custom Tag. Please try again.").queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, DeleteCustomTagSubcommand.class.getSimpleName());
			}
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
