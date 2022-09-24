package net.javadiscord.javabot.systems.staff_commands.embeds;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.components.ModalHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
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
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h3>This class represents the /embed edit command.</h3>
 */
public class EditEmbedSubcommand extends EmbedSubcommand implements ModalHandler {
	/**
	 * The {@link Modal}s id for editing an embed.
	 */
	public static final String EDIT_EMBED_ID = "edit-embed";
	/**
	 * A static cache of {@link MessageEmbed}s.
	 */
	protected static final Map<Long, Pair<Message, MessageEmbed>> EMBED_MESSAGE_CACHE;
	private static final String AUTHOR_ID = "AUTHOR";
	private static final String TITLE_DESC_COLOR_ID = "TITLE_DESC_COLOR";
	private static final String IMG_THUMB_ID = "IMG_THUMB";
	private static final String FOOTER_TIMESTAMP_ID = "FOOTER_TIMESTAMP";

	static {
		EMBED_MESSAGE_CACHE = new HashMap<>();
	}

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public EditEmbedSubcommand() {
		setSubcommandData(new SubcommandData("edit", "Edits a single embed message.")
				.addOptions(
						new OptionData(OptionType.STRING, "message-id", "The embed's message id.", true),
						new OptionData(OptionType.STRING, "type", "What exactly should be edited?", true)
								.addChoice("Author", AUTHOR_ID)
								.addChoice("Title / Description / Color", TITLE_DESC_COLOR_ID)
								.addChoice("Image / Thumbnail", IMG_THUMB_ID)
								.addChoice("Footer / Timestamp", FOOTER_TIMESTAMP_ID),
						new OptionData(OptionType.CHANNEL, "channel", "What channel is your embed in? If left empty, this defaults to the current one.", false)
								.setChannelTypes(ChannelType.TEXT)
				)
		);
	}

	protected static @Nullable String getValue(@NotNull String s) {
		return s.trim().isEmpty() ? null : s.trim();
	}

	@Override
	protected void handleEmbedSubcommand(SlashCommandInteractionEvent event, long messageId, GuildMessageChannel channel) {
		OptionMapping typeMapping = event.getOption("type");
		if (typeMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		String type = typeMapping.getAsString();
		channel.retrieveMessageById(messageId).queue(
				message -> {
					// just get the first embed
					if (message.getEmbeds().isEmpty()) {
						Responses.error(event.getHook(), "The provided message does not have any embeds attached. Please try again.").queue();
						return;
					}
					MessageEmbed embed = message.getEmbeds().get(0);
					EMBED_MESSAGE_CACHE.put(messageId, new Pair<>(message, embed));
					Modal modal = switch (type) {
						case AUTHOR_ID -> buildAuthorModal(embed, messageId);
						case TITLE_DESC_COLOR_ID -> buildTitleDescColorModal(embed, messageId);
						case IMG_THUMB_ID -> buildImageThumbnailModal(embed, messageId);
						case FOOTER_TIMESTAMP_ID -> buildFooterTimestampModal(embed, messageId);
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
	public void handleModal(@NotNull ModalInteractionEvent event, @NotNull List<ModalMapping> values) {
		event.deferReply(true).queue();
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event.getHook()).queue();
			return;
		}
		String[] id = ComponentIdBuilder.split(event.getModalId());
		Pair<Message, MessageEmbed> pair = EMBED_MESSAGE_CACHE.get(Long.parseLong(id[2]));
		if (pair == null) {
			Responses.error(event.getHook(), "An unexpected error occurred. Please try again.").queue();
			return;
		}
		Pair<String, MessageEditAction> action = switch (id[1]) {
			case AUTHOR_ID -> handleAuthorModal(event, pair.first(), pair.second());
			case TITLE_DESC_COLOR_ID -> handleTitleDescColorModal(event, pair.first(), pair.second());
			case IMG_THUMB_ID -> handleImageThumbnailModal(event, pair.first(), pair.second());
			case FOOTER_TIMESTAMP_ID -> handleFooterTimestampModal(event, pair.first(), pair.second());
			default -> new Pair<>("Unknown edit-type", null);
		};
		if (action.second() != null) {
			action.second().queue(
					success -> Responses.info(event.getHook(), "Embed Edited", "Successfully edited %s", pair.first().getJumpUrl()).queue(),
					err -> Responses.error(event.getHook(), "Something went wrong. Please check your inputs and try again.").queue()
			);
		} else {
			if (action.first().isEmpty()) {
				Responses.error(event.getHook(), "Something went wrong. Please check your inputs and try again.").queue();
			} else {
				Responses.error(event.getHook(), action.first()).queue();
			}
		}
		EMBED_MESSAGE_CACHE.remove(pair.first().getIdLong());
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
		return Modal.create(ComponentIdBuilder.build(EDIT_EMBED_ID, AUTHOR_ID, messageId), "Edit Author")
				.addActionRows(ActionRow.of(authorNameInput), ActionRow.of(authorUrlInput), ActionRow.of(authorIconUrlInput))
				.build();
	}

	private @NotNull Modal buildTitleDescColorModal(@NotNull MessageEmbed embed, long messageId) {
		TextInput titleInput = TextInput.create("title", "Title", TextInputStyle.SHORT)
				.setPlaceholder(String.format("Choose a fitting title. (max. %s chars)", MessageEmbed.TITLE_MAX_LENGTH))
				.setValue(embed.getTitle())
				.setMaxLength(MessageEmbed.TEXT_MAX_LENGTH)
				.setRequired(false)
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
				.setRequired(false)
				.build();
		TextInput colorInput = TextInput.create("color", "Hex Color (optional)", TextInputStyle.SHORT)
				.setPlaceholder("#FFFFFF")
				.setValue(embed.getColor() == null ? null : "#" + Integer.toHexString(embed.getColor().getRGB()).substring(2).toUpperCase())
				.setRequiredRange(7, 7)
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build(EDIT_EMBED_ID, TITLE_DESC_COLOR_ID, messageId), "Edit Title, Title URL, Description & Color")
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
		TextInput thumbnailUrlInput = TextInput.create("thumbnail-url", "Thumbnail URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder("https://example.com/example.png")
				.setValue(embed.getThumbnail() == null ? null : embed.getThumbnail().getUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build(EDIT_EMBED_ID, IMG_THUMB_ID, messageId), "Edit Image & Thumbnail")
				.addActionRows(ActionRow.of(imageUrlInput), ActionRow.of(thumbnailUrlInput))
				.build();
	}

	private @NotNull Modal buildFooterTimestampModal(@NotNull MessageEmbed embed, long messageId) {
		MessageEmbed.Footer footer = embed.getFooter();
		TextInput footerTextInput = TextInput.create("footer-text", "Footer Text (optional)", TextInputStyle.PARAGRAPH)
				.setPlaceholder(String.format("The footer's text. (max. %s chars)", MessageEmbed.TEXT_MAX_LENGTH))
				.setValue(footer == null ? null : footer.getText())
				.setMaxLength(MessageEmbed.TEXT_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput footerIconUrlInput = TextInput.create("footer-iconurl", "Footer Icon URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder(String.format("The footer's icon url. (max. %s chars)", MessageEmbed.URL_MAX_LENGTH))
				.setValue(footer == null ? null : footer.getIconUrl())
				.setMaxLength(MessageEmbed.URL_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput timestampInput = TextInput.create("timestamp", "Timestamp (optional)", TextInputStyle.SHORT)
				.setPlaceholder("The embed's timestamp, as an epoch milli")
				.setValue(embed.getTimestamp() == null ? null : String.valueOf(embed.getTimestamp().toEpochSecond() * 1000))
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build(EDIT_EMBED_ID, FOOTER_TIMESTAMP_ID, messageId), "Edit Footer & Timestamp")
				.addActionRows(ActionRow.of(footerTextInput), ActionRow.of(footerIconUrlInput), ActionRow.of(timestampInput))
				.build();
	}

	@Contract("_, _, _ -> new")
	private @NotNull Pair<String, MessageEditAction> handleAuthorModal(@NotNull ModalInteractionEvent event, @NotNull Message message, MessageEmbed embed) {
		ModalMapping authorNameMapping = event.getValue("author-name");
		ModalMapping authorUrlMapping = event.getValue("author-url");
		ModalMapping authorIconUrlMapping = event.getValue("author-iconurl");
		if (authorNameMapping == null || authorUrlMapping == null || authorIconUrlMapping == null) {
			return new Pair<>("Missing required arguments.", null);
		}
		String authorName = authorNameMapping.getAsString();
		String authorUrl = authorUrlMapping.getAsString();
		String authorIconUrl = authorIconUrlMapping.getAsString();
		if (authorName.isEmpty() && embed.getTitle() == null && embed.getDescription() == null) {
			return new Pair<>("Title, Author and Description may not be all empty. Set at least one.", null);
		}
		if (authorName.isEmpty() && !authorUrl.isEmpty()) {
			return new Pair<>("You cannot set an author url without the author name.", null);
		}
		if (authorName.isEmpty() && !authorIconUrl.isEmpty()) {
			return new Pair<>("You cannot set an author iconurl without the author name.", null);
		}
		if (!authorUrl.isEmpty() && !Checks.checkUrl(authorUrl)) {
			return new Pair<>("Please provide a valid author url.", null);
		}
		if (!authorIconUrl.isEmpty() && !Checks.checkImageUrl(authorIconUrl)) {
			return new Pair<>("Please provide a valid author icon url.", null);
		}
		EmbedBuilder builder = new EmbedBuilder(embed);
		builder.setAuthor(getValue(authorName), getValue(authorUrl), getValue(authorIconUrl));
		return new Pair<>("", message.editMessageEmbeds(builder.build()));
	}

	@Contract("_, _, _ -> new")
	private @NotNull Pair<String, MessageEditAction> handleTitleDescColorModal(@NotNull ModalInteractionEvent event, @NotNull Message message, MessageEmbed embed) {
		ModalMapping titleMapping = event.getValue("title");
		ModalMapping titleUrlMapping = event.getValue("title-url");
		ModalMapping descriptionMapping = event.getValue("description");
		ModalMapping colorMapping = event.getValue("color");
		if (titleMapping == null || titleUrlMapping == null || descriptionMapping == null || colorMapping == null) {
			return new Pair<>("Missing required arguments.", null);
		}
		String title = titleMapping.getAsString();
		String titleUrl = titleUrlMapping.getAsString();
		String description = descriptionMapping.getAsString();
		String color = colorMapping.getAsString();
		if (title.isEmpty() && !titleUrl.isEmpty()) {
			return new Pair<>("You cannot set a title url without a title.", null);
		}
		if (!titleUrl.isEmpty() && !Checks.checkUrl(titleUrl)) {
			return new Pair<>("Please provide a valid title url.", null);
		}
		if (title.isEmpty() && description.isEmpty() && embed.getAuthor() == null) {
			return new Pair<>("Title, Author and Description may not be all empty. Set at least one.", null);
		}
		if (!color.isEmpty() && Checks.HEX_PATTERN.matcher(color).matches() && !Checks.checkColor(color)) {
			return new Pair<>("Please provide a valid hex color.", null);
		}
		EmbedBuilder builder = new EmbedBuilder(embed)
				.setTitle(getValue(title), getValue(titleUrl))
				.setDescription(getValue(description))
				.setColor(color.isEmpty() ? null : Color.decode(color));
		return new Pair<>("", message.editMessageEmbeds(builder.build()));
	}

	@Contract("_, _, _ -> new")
	private @NotNull Pair<String, MessageEditAction> handleImageThumbnailModal(@NotNull ModalInteractionEvent event, @NotNull Message message, MessageEmbed embed) {
		ModalMapping imageUrlMapping = event.getValue("image-url");
		ModalMapping thumbnailUrlMapping = event.getValue("thumbnail-url");
		if (imageUrlMapping == null || thumbnailUrlMapping == null) {
			return new Pair<>("Missing required arguments.", null);
		}
		String imageUrl = imageUrlMapping.getAsString();
		String thumbnailUrl = thumbnailUrlMapping.getAsString();
		if (!imageUrl.isEmpty() && !Checks.checkImageUrl(imageUrl)) {
			return new Pair<>("Please provide a valid image url.", null);
		}
		if (!thumbnailUrl.isEmpty() && !Checks.checkImageUrl(thumbnailUrl)) {
			return new Pair<>("Please provide a valid thumbnail url.", null);
		}
		EmbedBuilder builder = new EmbedBuilder(embed)
				.setImage(getValue(imageUrl))
				.setThumbnail(getValue(thumbnailUrl));
		return new Pair<>("", message.editMessageEmbeds(builder.build()));
	}

	@Contract("_, _, _ -> new")
	private @NotNull Pair<String, MessageEditAction> handleFooterTimestampModal(@NotNull ModalInteractionEvent event, @NotNull Message message, MessageEmbed embed) {
		ModalMapping footerTextMapping = event.getValue("footer-text");
		ModalMapping footerIconUrlMapping = event.getValue("footer-iconurl");
		ModalMapping timestampMapping = event.getValue("timestamp");
		if (footerTextMapping == null || footerIconUrlMapping == null || timestampMapping == null) {
			return new Pair<>("Missing required arguments.", null);
		}
		String footerText = footerTextMapping.getAsString();
		String footerIconUrl = footerIconUrlMapping.getAsString();
		String timestamp = timestampMapping.getAsString();
		if (footerText.isEmpty() && !footerIconUrl.isEmpty()) {
			return new Pair<>("You cannot set a footer iconurl without the footer text", null);
		}
		EmbedBuilder builder = new EmbedBuilder(embed)
				.setFooter(getValue(footerText), getValue(footerIconUrl))
				.setTimestamp(getValue(timestamp) == null ? null : Instant.ofEpochMilli(Long.parseLong(timestamp)));
		return new Pair<>("", message.editMessageEmbeds(builder.build()));
	}
}
