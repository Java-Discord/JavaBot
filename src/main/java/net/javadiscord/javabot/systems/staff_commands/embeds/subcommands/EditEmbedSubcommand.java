package net.javadiscord.javabot.systems.staff_commands.embeds.subcommands;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EditEmbedSubcommand extends SlashCommand.Subcommand {
	public EditEmbedSubcommand() {
		setSubcommandData(new SubcommandData("edit", "Edits a single embed message.")
				.addOptions(
						new OptionData(OptionType.STRING, "message-id", "The embed's message id.", true),
						new OptionData(OptionType.STRING, "type", "What exactly should be edited?", true)
								.addChoice("Author", "AUTHOR")
								.addChoice("Title / Description / Color", "TITLE_DESC_COLOR")
								.addChoice("Image / Thumbnail", "IMG_THUMB")
								.addChoice("Footer / Timestamp", "FOOTER_TIMESTAMP"),
						new OptionData(OptionType.CHANNEL, "channel", "What channel is your embed in? If left empty, this defaults to the current one.", true)
								.setChannelTypes(ChannelType.TEXT)
				)
		);
		handleModalIds("embed-edit");
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("message-id");
		OptionMapping typeMapping = event.getOption("type");
		if (idMapping == null || typeMapping == null || !Checks.checkLongInput(idMapping)) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		if (Checks.checkGuild(event)) {
			Responses.error(event, "This command may only be used inside of a server.").queue();
			return;
		}
		long messageId = idMapping.getAsLong();
		String type = typeMapping.getAsString();
		TextChannel channel = event.getOption("channel", event.getTextChannel(), OptionMapping::getAsTextChannel);
		channel.retrieveMessageById(messageId).queue(
				message -> {
					// just get the first embed
					if (message.getEmbeds().isEmpty()) {
						Responses.error(event.getHook(), "The provided message does not have any embeds attached. Please try again.").queue();
						return;
					}
					MessageEmbed embed = message.getEmbeds().get(0);
					// TODO: add missing types
					Modal modal = switch (type) {
						case "TITLE_DESC_COLOR" -> buildTitleDescColorModal(embed, messageId);
					};
					event.replyModal(modal).queue();
				},
				err -> event.reply("Could not edit Embed Message. Please try again.").queue()
		);
	}

	@Override
	public void handleModal(ModalInteractionEvent event, List<ModalMapping> values) {
		event.deferReply(true).queue();
		if (Checks.checkGuild(event)) {
			Responses.error(event.getHook(), "This command may only be used inside of a server.").queue();
			return;
		}
	}

	private @NotNull Modal buildTitleDescColorModal(@NotNull MessageEmbed embed, long messageId) {
		TextInput titleInput = TextInput.create("title", "Title", TextInputStyle.SHORT)
				.setPlaceholder(String.format("Choose a fitting Title. (max. %s chars)", MessageEmbed.TITLE_MAX_LENGTH))
				.setValue(embed.getTitle())
				.setMaxLength(MessageEmbed.TEXT_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput titleUrlInput = TextInput.create("title-url", "Title URL", TextInputStyle.SHORT)
				.setPlaceholder(String.format("The Title's URL. (max. %s chars)", MessageEmbed.URL_MAX_LENGTH))
				.setValue(embed.getUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput descriptionInput = TextInput.create("description", "Description", TextInputStyle.PARAGRAPH)
				.setPlaceholder(String.format("Choose a Description for your Embed Message. (max. %s chars)", MessageEmbed.DESCRIPTION_MAX_LENGTH))
				.setValue(embed.getDescription())
				.setMaxLength(MessageEmbed.DESCRIPTION_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput colorInput = TextInput.create("color", "Hex Color (optional)", TextInputStyle.SHORT)
				.setPlaceholder("#FFFFFF")
				.setValue("#" + Integer.toHexString(embed.getColor().getRGB()).toUpperCase())
				.setMaxLength(7)
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build("embed-edit", "TITLE_DESC_COLOR", messageId), "Edit Title, Description & Color")
				.addActionRows(ActionRow.of(titleInput), ActionRow.of(titleUrlInput), ActionRow.of(descriptionInput), ActionRow.of(colorInput))
				.build();
	}
}
