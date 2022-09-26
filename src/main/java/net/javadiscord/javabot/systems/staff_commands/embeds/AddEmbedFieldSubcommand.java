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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This class represents the `/embed create` command.
 */
@AutoDetectableComponentHandler("embed-addfield")
public class AddEmbedFieldSubcommand extends EmbedSubcommand implements ModalHandler {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public AddEmbedFieldSubcommand(BotConfig botConfig) {
		super(botConfig);
		setSubcommandData(new SubcommandData("add-field", "Adds a field to an embed message.")
				.addOptions(
						new OptionData(OptionType.STRING, "message-id", "The embed's message id.", true),
						new OptionData(OptionType.CHANNEL, "channel", "What channel is the embed in?", false)
								.setChannelTypes(ChannelType.TEXT, ChannelType.VOICE, ChannelType.GUILD_PRIVATE_THREAD, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_NEWS_THREAD)
				)
		);
	}

	@Override
	protected void handleEmbedSubcommand(SlashCommandInteractionEvent event, long messageId, GuildMessageChannel channel) {
		channel.retrieveMessageById(messageId).queue(message -> {
			// just get the first embed
			if (message.getEmbeds().isEmpty()) {
				Responses.error(event.getHook(), "The provided message does not have any embeds attached. Please try again.").queue();
				return;
			}
			MessageEmbed embed = message.getEmbeds().get(0);
			EditEmbedSubcommand.EMBED_MESSAGE_CACHE.put(messageId, new Pair<>(message, embed));
			event.replyModal(buildAddFieldModal(messageId)).queue();
		});
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, @NotNull List<ModalMapping> values) {
		event.deferReply(true).queue();
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event.getHook()).queue();
			return;
		}
		String[] id = ComponentIdBuilder.split(event.getModalId());
		Pair<Message, MessageEmbed> pair = EditEmbedSubcommand.EMBED_MESSAGE_CACHE.get(Long.parseLong(id[1]));
		if (pair == null) {
			Responses.error(event.getHook(), "An unexpected error occurred. Please try again.").queue();
			return;
		}
		Pair<String, MessageEditAction> action = handleAddField(event, pair.first(), pair.second());
		if (action.second() != null) {
			action.second().queue(
					success -> Responses.info(event.getHook(), "Embed Field Added", "Successfully added the field to %s", pair.first().getJumpUrl()).queue(),
					err -> Responses.error(event.getHook(), "Something went wrong. Please check your inputs and try again.").queue()
			);
		} else {
			if (action.first().isEmpty()) {
				Responses.error(event.getHook(), "Something went wrong. Please check your inputs and try again.").queue();
			} else {
				Responses.error(event.getHook(), action.first()).queue();
			}
		}
		EditEmbedSubcommand.EMBED_MESSAGE_CACHE.remove(pair.first().getIdLong());
	}

	private @NotNull Modal buildAddFieldModal(long messageId) {
		TextInput titleInput = TextInput.create("name", "Field Name", TextInputStyle.SHORT)
				.setPlaceholder(String.format("Choose a fitting field name. (max. %s chars)", MessageEmbed.TITLE_MAX_LENGTH))
				.setMaxLength(MessageEmbed.TITLE_MAX_LENGTH)
				.setRequired(true)
				.build();
		TextInput valueInput = TextInput.create("value", "Field Value", TextInputStyle.PARAGRAPH)
				.setPlaceholder(String.format("Choose a description for your embed. (max. %s chars)", MessageEmbed.VALUE_MAX_LENGTH))
				.setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
				.setRequired(true)
				.build();
		TextInput inlineInput = TextInput.create("inline", "Should the field inline?", TextInputStyle.SHORT)
				.setPlaceholder("true")
				.setMaxLength(5)
				.setRequired(true)
				.build();
		return Modal.create(ComponentIdBuilder.build("embed-addfield", messageId), "Add an Embed Field")
				.addActionRows(ActionRow.of(titleInput), ActionRow.of(valueInput), ActionRow.of(inlineInput))
				.build();
	}

	private @NotNull Pair<String, MessageEditAction> handleAddField(@NotNull ModalInteractionEvent event, Message message, MessageEmbed embed) {
		ModalMapping nameMapping = event.getValue("name");
		ModalMapping valueMapping = event.getValue("value");
		ModalMapping inlineMapping = event.getValue("inline");
		if (nameMapping == null || valueMapping == null || inlineMapping == null) {
			return new Pair<>("Missing required arguments", null);
		}
		String name = nameMapping.getAsString();
		String value = valueMapping.getAsString();
		boolean inline = Boolean.parseBoolean(inlineMapping.getAsString());
		if (name.isEmpty() || value.isEmpty()) {
			return new Pair<>("You need to set both the name and the value of the field!", null);
		}
		EmbedBuilder builder = new EmbedBuilder(embed)
				.addField(name, value, inline);
		return new Pair<>("", message.editMessageEmbeds(builder.build()));
	}
}
