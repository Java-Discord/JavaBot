package net.javadiscord.javabot.systems.staff_commands.embeds.subcommands;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
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

import java.awt.*;
import java.util.List;

public class CreateEmbedSubcommand extends SlashCommand.Subcommand {
	public CreateEmbedSubcommand() {
		setSubcommandData(new SubcommandData("create", "Creates a new basic embed message.")
				.addOptions(
						new OptionData(OptionType.CHANNEL, "channel", "What channel should the embed be sent to?", false)
								.setChannelTypes(ChannelType.TEXT)
				)
		);
		handleModalIds("embed-create");
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (Checks.checkGuild(event)) {
			Responses.error(event, "This command may only be used inside of a server.").queue();
			return;
		}
		event.replyModal(buildBasicEmbedCreateModal(event.getOption("channel", event.getTextChannel(), OptionMapping::getAsTextChannel))).queue();
	}

	@Override
	public void handleModal(ModalInteractionEvent event, List<ModalMapping> values) {
		event.deferReply(true).queue();
		if (Checks.checkGuild(event)) {
			Responses.error(event.getHook(), "This command may only be used inside of a server.").queue();
			return;
		}
		String[] id = ComponentIdBuilder.split(event.getModalId());
		TextChannel channel = event.getGuild().getTextChannelById(id[1]);
		if (channel == null) {
			Responses.error(event.getHook(), "Please provide a valid text channel.").queue();
			return;
		}
		EmbedBuilder builder = buildBasicEmbed(event);
		if (builder.isEmpty() || !builder.isValidLength()) {
			Responses.error(event.getHook(), "You've provided an invalid embed!").queue();
			return;
		}
		channel.sendMessageEmbeds(builder.build()).queue();
		event.getHook().sendMessage("Done!").queue();
	}

	private @NotNull Modal buildBasicEmbedCreateModal(@NotNull TextChannel channel) {
		TextInput titleInput = TextInput.create("title", "Title", TextInputStyle.SHORT)
				.setPlaceholder(String.format("Choose a fitting Title. (max. %s chars)", MessageEmbed.TITLE_MAX_LENGTH))
				.setMaxLength(MessageEmbed.TITLE_MAX_LENGTH)
				.setRequired(false)
				.build();
		TextInput descriptionInput = TextInput.create("description", "Description", TextInputStyle.PARAGRAPH)
				.setPlaceholder(String.format("Choose a Description for your Embed Message. (max. %s chars)", MessageEmbed.DESCRIPTION_MAX_LENGTH))
				.setMaxLength(MessageEmbed.DESCRIPTION_MAX_LENGTH)
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
				.addActionRows(ActionRow.of(titleInput), ActionRow.of(descriptionInput), ActionRow.of(colorInput), ActionRow.of(imageInput))
				.build();
	}

	private EmbedBuilder buildBasicEmbed(ModalInteractionEvent event) {
		EmbedBuilder builder = new EmbedBuilder();
		ModalMapping titleMapping = event.getValue("title");
		ModalMapping descriptionMapping = event.getValue("description");
		ModalMapping colorMapping = event.getValue("color");
		ModalMapping imageMapping = event.getValue("image");
		if (titleMapping != null) {
			builder.setTitle(titleMapping.getAsString());
		}
		if (descriptionMapping != null) {
			builder.setDescription(descriptionMapping.getAsString());
		}
		if (colorMapping != null && Checks.checkHexColor(colorMapping.getAsString())) {
			builder.setColor(Color.decode(colorMapping.getAsString()));
		}
		if (imageMapping != null && Checks.checkImageUrl(imageMapping.getAsString())) {
			builder.setImage(imageMapping.getAsString());
		}
		return builder;
	}
}
