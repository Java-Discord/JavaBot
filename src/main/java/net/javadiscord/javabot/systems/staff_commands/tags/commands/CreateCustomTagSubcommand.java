package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import net.javadiscord.javabot.util.UserUtils;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
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
import net.javadiscord.javabot.systems.staff_commands.tags.model.CustomTag;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.List;

/**
 * Administrative Subcommand that allows to create {@link CustomTag}s.
 */
@AutoDetectableComponentHandler("tag-create")
public class CreateCustomTagSubcommand extends TagsSubcommand implements ModalHandler {
	private final CustomTagManager tagManager;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param tagManager The {@link CustomTagManager}
	 * @param botConfig The main configuration of the bot
	 */
	public CreateCustomTagSubcommand(CustomTagManager tagManager, BotConfig botConfig) {
		super(botConfig);
		this.tagManager = tagManager;
		setCommandData(new SubcommandData("create", "Creates a new Custom Tag."));
	}

	@Override
	public InteractionCallbackAction<Void> handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		return event.replyModal(buildCreateCommandModal());
	}

	private @NotNull Modal buildCreateCommandModal() {
		TextInput nameField = TextInput.create("tag-name", "Tag Name", TextInputStyle.SHORT)
				.setPlaceholder("bee-movie")
				.setMaxLength(CommandData.MAX_NAME_LENGTH)
				.setRequired(true)
				.build();
		TextInput responseField = TextInput.create("tag-response", "Tag Response", TextInputStyle.PARAGRAPH)
				.setPlaceholder("""
						According to all known laws
						of aviation,

						there is no way a bee
						should be able to fly...
						""")
				.setMaxLength(2000)
				.setRequired(true)
				.build();
		TextInput replyField = TextInput.create("tag-reply", "Should the tag reply to your message?", TextInputStyle.SHORT)
				.setPlaceholder("true")
				.setValue("true")
				.setMaxLength(5)
				.setRequired(true)
				.build();
		TextInput embedField = TextInput.create("tag-embed", "Should the tag be embedded?", TextInputStyle.SHORT)
				.setPlaceholder("true")
				.setValue("true")
				.setMaxLength(5)
				.setRequired(true)
				.build();
		return Modal.create("tag-create", "Create Custom Tag")
				.addActionRows(ActionRow.of(nameField), ActionRow.of(responseField), ActionRow.of(replyField), ActionRow.of(embedField))
				.build();
	}

	private @NotNull MessageEmbed buildCreateCommandEmbed(@NotNull User createdBy, @NotNull CustomTag command) {
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(createdBy), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Custom Tag Created")
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
	public void handleModal(@NotNull ModalInteractionEvent event, @NotNull List<ModalMapping> values) {
		event.deferReply().queue();
		ModalMapping nameMapping = event.getValue("tag-name");
		ModalMapping responseMapping = event.getValue("tag-response");
		ModalMapping replyMapping = event.getValue("tag-reply");
		ModalMapping embedMapping = event.getValue("tag-embed");
		if (nameMapping == null || responseMapping == null || replyMapping == null || embedMapping == null) {
			Responses.replyMissingArguments(event.getHook()).queue();
			return;
		}
		// build the CustomCommand object
		CustomTag command = new CustomTag();
		command.setGuildId(event.getGuild().getIdLong());
		command.setCreatedBy(event.getUser().getIdLong());
		command.setName(nameMapping.getAsString());
		command.setResponse(responseMapping.getAsString());
		command.setReply(Boolean.parseBoolean(replyMapping.getAsString()));
		command.setEmbed(Boolean.parseBoolean(embedMapping.getAsString()));
		try {
			if (tagManager.addCommand(event.getGuild(), command)) {
				event.getHook().sendMessageEmbeds(buildCreateCommandEmbed(event.getUser(), command)).queue();
			} else {
				Responses.error(event.getHook(), "Could not create Custom Tag. Please try again.").queue();
			}
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e);
			Responses.error(event.getHook(), "An unexpected error occurred. Please try again.").queue();
		}
	}
}
