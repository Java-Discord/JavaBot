package net.javadiscord.javabot.systems.staff_commands.embeds;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.interactions.components.ModalHandler;
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

/**
 * <h3>This class represents the /embed edit command.</h3>
 */
public class EditEmbedSubcommand extends SlashCommand.Subcommand implements ModalHandler {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public EditEmbedSubcommand() {
		setSubcommandData(new SubcommandData("edit", "Edits a single embed message.")
				.addOptions(
						new OptionData(OptionType.STRING, "message-id", "The embed's message id.", true),
						new OptionData(OptionType.STRING, "type", "What exactly should be edited?", true)
								.addChoice("Author", "AUTHOR")
								.addChoice("Title / Description / Color", "TITLE_DESC_COLOR")
								.addChoice("Image / Thumbnail", "IMG_THUMB")
								.addChoice("Footer / Timestamp", "FOOTER_TIMESTAMP"),
						new OptionData(OptionType.CHANNEL, "channel", "What channel is your embed in? If left empty, this defaults to the current one.", false)
								.setChannelTypes(ChannelType.TEXT)
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("message-id");
		OptionMapping typeMapping = event.getOption("type");
		if (idMapping == null || typeMapping == null || Checks.isInvalidLongInput(idMapping)) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasStaffRole(event.getGuild(), event.getMember())) {
			Responses.replyStaffOnly(event, event.getGuild()).queue();
			return;
		}
		long messageId = idMapping.getAsLong();
		String type = typeMapping.getAsString();
		TextChannel channel = event.getOption("channel", event.getChannel().asTextChannel(), m -> m.getAsChannel().asTextChannel());
		channel.retrieveMessageById(messageId).queue(
				message -> {
					// just get the first embed
					if (message.getEmbeds().isEmpty()) {
						Responses.error(event.getHook(), "The provided message does not have any embeds attached. Please try again.").queue();
						return;
					}
					MessageEmbed embed = message.getEmbeds().get(0);
					Modal modal = switch (type) {
						case "AUTHOR" -> buildAuthorModal(embed, messageId);
						case "TITLE_DESC_COLOR" -> buildTitleDescColorModal(embed, messageId);
						case "IMG_THUMB" -> buildImageThumbnailModal(embed, messageId);
						case "FOOTER_TIMESTAMP" -> buildFooterTimestampModal(embed, messageId);
						default -> null;
					};
					if (modal == null) {
						Responses.error(event, "Please select a valid edit-type!").queue();
					} else {
						event.replyModal(modal).queue();
					}
				},
				err -> event.reply("Could not edit Embed Message. Please try again.").queue()
		);
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, List<ModalMapping> values) {
		event.deferReply(true).queue();
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event.getHook()).queue();
			return;
		}
	}

	private @NotNull Modal buildAuthorModal(@NotNull MessageEmbed embed, long messageId) {
		MessageEmbed.AuthorInfo info = embed.getAuthor();
		TextInput authorNameInput = TextInput.create("author-name", "Author Name (optional)", TextInputStyle.SHORT)
				.setPlaceholder(String.format("Choose the author's name. (max. %s chars)", MessageEmbed.AUTHOR_MAX_LENGTH))
				.setValue(info == null ? null : info.getName())
				.setMaxLength(MessageEmbed.AUTHOR_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput authorUrlInput = TextInput.create("author-url", "Author URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder(String.format("The author's url. (max. %s chars)", MessageEmbed.URL_MAX_LENGTH))
				.setValue(info == null ? null : info.getUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput authorIconUrlInput = TextInput.create("author-iconurl", "Author Icon URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder(String.format("The author's icon url. (max. %s chars)", MessageEmbed.URL_MAX_LENGTH))
				.setValue(info == null ? null : info.getIconUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build("embed-edit", "AUTHOR", messageId), "Edit Author")
				.addActionRows(ActionRow.of(authorNameInput), ActionRow.of(authorUrlInput), ActionRow.of(authorIconUrlInput))
				.build();
	}

	private @NotNull Modal buildTitleDescColorModal(@NotNull MessageEmbed embed, long messageId) {
		TextInput titleInput = TextInput.create("title", "Title", TextInputStyle.SHORT)
				.setPlaceholder(String.format("Choose a fitting title. (max. %s chars)", MessageEmbed.TITLE_MAX_LENGTH))
				.setValue(embed.getTitle())
				.setMaxLength(MessageEmbed.TEXT_MAX_LENGTH)
				.setRequired(true)
				.build();
		TextInput titleUrlInput = TextInput.create("title-url", "Title URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder(String.format("The title's url. (max. %s chars)", MessageEmbed.URL_MAX_LENGTH))
				.setValue(embed.getUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput descriptionInput = TextInput.create("description", "Description", TextInputStyle.PARAGRAPH)
				.setPlaceholder("Choose a description for your embed.")
				.setValue(embed.getDescription())
				.setRequired(true)
				.build();
		TextInput colorInput = TextInput.create("color", "Hex Color (optional)", TextInputStyle.SHORT)
				.setPlaceholder("#FFFFFF")
				.setValue(embed.getColor() == null ? null : "#" + Integer.toHexString(embed.getColor().getRGB()).toUpperCase())
				.setMaxLength(7)
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build("embed-edit", "TITLE_DESC_COLOR", messageId), "Edit Title, Title URL, Description & Color")
				.addActionRows(ActionRow.of(titleInput), ActionRow.of(titleUrlInput), ActionRow.of(descriptionInput), ActionRow.of(colorInput))
				.build();
	}

	private @NotNull Modal buildImageThumbnailModal(@NotNull MessageEmbed embed, long messageId) {
		TextInput imageUrlInput = TextInput.create("image-url", "Image URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder("https://example.com/example.png")
				.setValue(embed.getImage() == null ? null : embed.getImage().getUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput thumbnailUrlInput = TextInput.create("thumb-url", "Thumbnail URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder("https://example.com/example.png")
				.setValue(embed.getThumbnail() == null ? null : embed.getThumbnail().getUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build("embed-edit", "IMG_THUMB", messageId), "Edit Image & Thumbnail")
				.addActionRows(ActionRow.of(imageUrlInput), ActionRow.of(thumbnailUrlInput))
				.build();
	}

	private @NotNull Modal buildFooterTimestampModal(@NotNull MessageEmbed embed, long messageId) {
		MessageEmbed.Footer footer = embed.getFooter();
		TextInput imageUrlInput = TextInput.create("footer-text", "Footer Text (optional)", TextInputStyle.SHORT)
				// TODO: check real length
				.setPlaceholder(String.format("The footer's text. (max. %s chars)", MessageEmbed.AUTHOR_MAX_LENGTH))
				.setValue(footer == null ? null : footer.getText())
				.setMaxLength(MessageEmbed.AUTHOR_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput footerIconUrlInput = TextInput.create("footer-iconurl", "Footer Icon URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder(String.format("The footer's icon url. (max. %s chars)", MessageEmbed.URL_MAX_LENGTH))
				.setValue(footer == null ? null : footer.getIconUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput timestampInput = TextInput.create("timestamp", "Timestamp (optional)", TextInputStyle.SHORT)
				.setPlaceholder(String.format("The embed's timestamp."))
				.setValue(embed.getTimestamp() == null ? null : String.valueOf(embed.getTimestamp().toEpochSecond()))
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build("embed-edit", "FOOTER_TIMESTAMP", messageId), "Edit Footer & Timestamp")
				.addActionRows(ActionRow.of(imageUrlInput), ActionRow.of(footerIconUrlInput), ActionRow.of(timestampInput))
				.build();
	}
}
