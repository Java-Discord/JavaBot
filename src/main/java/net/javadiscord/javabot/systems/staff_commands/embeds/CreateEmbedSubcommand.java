package net.javadiscord.javabot.systems.staff_commands.embeds;

import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

/**
 * This class represents the `/embed create` command.
 */
@AutoDetectableComponentHandler("embed-create")
public class CreateEmbedSubcommand extends SlashCommand.Subcommand implements ModalHandler {
	private final BotConfig botConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public CreateEmbedSubcommand(BotConfig botConfig) {
		this.botConfig = botConfig;
		setCommandData(new SubcommandData("create", "Creates a new basic embed message.")
				.addOptions(
						new OptionData(OptionType.CHANNEL, "channel", "What channel should the embed be sent to?", false)
								.setChannelTypes(ChannelType.TEXT, ChannelType.VOICE, ChannelType.GUILD_PRIVATE_THREAD, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_NEWS_THREAD)
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.replyStaffOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		GuildMessageChannel channel = event.getOption("channel", event.getChannel().asGuildMessageChannel(), m -> m.getAsChannel().asGuildMessageChannel());
		if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL)) {
			Responses.replyInsufficientPermissions(event, Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL).queue();
			return;
		}
		event.replyModal(buildBasicEmbedCreateModal(channel)).queue();
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, @NotNull List<ModalMapping> values) {
		event.deferReply(true).queue();
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event.getHook()).queue();
			return;
		}
		String[] id = ComponentIdBuilder.split(event.getModalId());
		String channelId = id[1];
		MessageChannel channel = event.getGuild().getTextChannelById(channelId);
		if (channel == null) {
			channel = event.getGuild().getThreadChannelById(channelId);
		}
		if (channel == null) {
			Responses.error(event.getHook(), "Please provide a valid text channel.").queue();
			return;
		}
		Pair<String, EmbedBuilder> pair = buildBasicEmbed(event);
		if (pair.second() == null) {
			Responses.error(event.getHook(), pair.first()).queue();
			return;
		}
		channel.sendMessageEmbeds(pair.second().build()).queue(
				s -> event.getHook().sendMessage("Done!").addActionRow(Button.link(s.getJumpUrl(), "Jump to Embed")).queue(),
				e -> Responses.error(event.getHook(), "Could not send embed: %s", e.getMessage()).queue()
		);
	}

	private @NotNull Modal buildBasicEmbedCreateModal(@NotNull Channel channel) {
		TextInput titleInput = TextInput.create("title", "Title", TextInputStyle.SHORT)
				.setPlaceholder(String.format("Choose a fitting title. (max. %s chars)", MessageEmbed.TITLE_MAX_LENGTH))
				.setMaxLength(MessageEmbed.TITLE_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput descriptionInput = TextInput.create("description", "Description", TextInputStyle.PARAGRAPH)
				.setPlaceholder("Choose a description for your embed.")
				.setRequired(false)
				.build();
		TextInput colorInput = TextInput.create("color", "Hex Color (optional)", TextInputStyle.SHORT)
				.setPlaceholder("#FFFFFF")
				.setMaxLength(7)
				.setRequired(false)
				.build();
		TextInput imageInput = TextInput.create("image", "Image URL (optional)", TextInputStyle.SHORT)
				.setPlaceholder("https://example.com/example.png")
				.setRequired(false)
				.build();
		return Modal.create(ComponentIdBuilder.build("embed-create", channel.getIdLong()), "Create an Embed Message")
				.addComponents(ActionRow.of(titleInput), ActionRow.of(descriptionInput), ActionRow.of(colorInput), ActionRow.of(imageInput))
				.build();
	}

	private @NotNull Pair<String, EmbedBuilder> buildBasicEmbed(@NotNull ModalInteractionEvent event) {
		ModalMapping titleMapping = event.getValue("title");
		ModalMapping descriptionMapping = event.getValue("description");
		ModalMapping colorMapping = event.getValue("color");
		ModalMapping imageMapping = event.getValue("image");
		if (titleMapping == null || descriptionMapping == null || colorMapping == null || imageMapping == null) {
			return new Pair<>("Missing required arguments.", null);
		}
		String title = titleMapping.getAsString();
		String description = descriptionMapping.getAsString();
		String color = colorMapping.getAsString();
		String imageUrl = imageMapping.getAsString();
		if (title.isEmpty() && description.isEmpty()) {
			return new Pair<>("You need to either set a title or a description!", null);
		}
		if (!color.isEmpty() && !Checks.checkHexColor(colorMapping.getAsString())) {
			return new Pair<>("Please provide a valid hex color!", null);
		}
		if (!imageUrl.isEmpty() && !Checks.checkImageUrl(imageMapping.getAsString())) {
			return new Pair<>("Please provide a valid image url!", null);
		}
		EmbedBuilder builder = new EmbedBuilder()
				.setTitle(EditEmbedSubcommand.getValue(title))
				.setDescription(EditEmbedSubcommand.getValue(description))
				.setColor(color.isEmpty() ? null : Color.decode(color))
				.setImage(EditEmbedSubcommand.getValue(imageUrl));
		return new Pair<>("", builder);
	}
}
