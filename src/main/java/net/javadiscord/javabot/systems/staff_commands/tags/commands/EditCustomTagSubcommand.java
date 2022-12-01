package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.staff_commands.tags.CustomTagManager;
import net.javadiscord.javabot.systems.staff_commands.tags.dao.CustomTagRepository;
import net.javadiscord.javabot.systems.staff_commands.tags.model.CustomTag;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * <h3>This class represents the /tag-admin edit command.</h3>
 */
@AutoDetectableComponentHandler("tag-edit")
public class EditCustomTagSubcommand extends TagsSubcommand implements AutoCompletable, ModalHandler {
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
	public EditCustomTagSubcommand(CustomTagManager customTagManager, BotConfig botConfig, CustomTagRepository customTagRepository, ExecutorService asyncPool) {
		super(botConfig);
		this.customTagManager = customTagManager;
		this.asyncPool = asyncPool;
		this.customTagRepository = customTagRepository;
		setCommandData(new SubcommandData("edit", "Edits a single Custom Tag.")
				.addOption(OptionType.STRING, "name", "The tag's name.", true, true)
		);
	}

	@Override
	public InteractionCallbackAction<?> handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping nameMapping = event.getOption("name");
		if (nameMapping == null) {
			return Responses.replyMissingArguments(event);
		}
		if (event.getGuild() == null) {
			return Responses.replyGuildOnly(event);
		}
		Set<CustomTag> tags = customTagManager.getLoadedCommands(event.getGuild().getIdLong());
		Optional<CustomTag> tagOptional = tags.stream()
				.filter(t -> t.getName().equalsIgnoreCase(nameMapping.getAsString()))
				.findFirst();
		if (tagOptional.isPresent()) {
			return event.replyModal(buildEditTagModal(tagOptional.get()));
		}
		return Responses.error(event, "Could not find tag with name: `%s`", nameMapping.getAsString());
	}

	private @NotNull Modal buildEditTagModal(@NotNull CustomTag tag) {
		TextInput responseField = TextInput.create("tag-response", "Tag Response", TextInputStyle.PARAGRAPH)
				.setPlaceholder("""
						According to all known laws
						of aviation,

						there is no way a bee
						should be able to fly...
						""")
				.setValue(tag.getResponse())
				.setMaxLength(2000)
				.setRequired(true)
				.build();
		TextInput replyField = TextInput.create("tag-reply", "Should the tag reply to your message?", TextInputStyle.SHORT)
				.setPlaceholder("true")
				.setValue(String.valueOf(tag.isReply()))
				.setMaxLength(5)
				.setRequired(true)
				.build();
		TextInput embedField = TextInput.create("tag-embed", "Should the tag be embedded?", TextInputStyle.SHORT)
				.setPlaceholder("true")
				.setValue(String.valueOf(tag.isReply()))
				.setMaxLength(5)
				.setRequired(true)
				.build();
		return Modal.create(ComponentIdBuilder.build("tag-edit", tag.getName()),
						String.format("Edit \"%s\"", tag.getName().length() > 90 ? tag.getName().substring(0, 87) + "..." : tag.getName()))
				.addActionRows(ActionRow.of(responseField), ActionRow.of(replyField), ActionRow.of(embedField))
				.build();
	}

	private @NotNull MessageEmbed buildEditTagEmbed(@NotNull Member createdBy, @NotNull CustomTag command) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getUser().getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Custom Tag Edited")
				.addField("Id", String.format("`%s`", command.getId()), true)
				.addField("Name", String.format("`%s`", command.getName()), true)
				.addField("Created by", createdBy.getAsMention(), true)
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

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, @NotNull List<ModalMapping> values) {
		event.deferReply().queue();
		String[] id = ComponentIdBuilder.split(event.getModalId());
		ModalMapping responseMapping = event.getValue("tag-response");
		ModalMapping replyMapping = event.getValue("tag-reply");
		ModalMapping embedMapping = event.getValue("tag-embed");
		if (responseMapping == null || replyMapping == null || embedMapping == null) {
			Responses.replyMissingArguments(event.getHook()).queue();
			return;
		}
		if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
			Responses.error(event.getHook(), "This may only be used inside servers.").queue();
			return;
		}
		// build the CustomCommand object
		CustomTag update = new CustomTag();
		update.setGuildId(event.getGuild().getIdLong());
		update.setCreatedBy(event.getUser().getIdLong());
		update.setName(id[1]);
		update.setResponse(responseMapping.getAsString());
		update.setReply(Boolean.parseBoolean(replyMapping.getAsString()));
		update.setEmbed(Boolean.parseBoolean(embedMapping.getAsString()));

		event.deferReply(true).queue();
		asyncPool.execute(()->{
			try {
				Optional<CustomTag> tagOptional = customTagRepository.findByName(event.getGuild().getIdLong(), update.getName());
				if (tagOptional.isEmpty()) {
					Responses.error(event.getHook(), "Could not find Custom Tag with name `/%s`.", update.getName()).queue();
					return;
				}
				if (customTagManager.editCommand(event.getGuild().getIdLong(), tagOptional.get(), update)) {
					event.getHook().sendMessageEmbeds(buildEditTagEmbed(event.getMember(), update)).queue();
					return;
				}
				Responses.error(event.getHook(), "Could not edit Custom Command. Please try again.").queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, EditCustomTagSubcommand.class.getSimpleName());
			}
		});
	}
}
