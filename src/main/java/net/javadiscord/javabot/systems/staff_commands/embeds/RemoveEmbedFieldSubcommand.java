package net.javadiscord.javabot.systems.staff_commands.embeds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents the `/embed create` command.
 */
public class RemoveEmbedFieldSubcommand extends EmbedSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public RemoveEmbedFieldSubcommand() {
		setSubcommandData(new SubcommandData("remove-field", "Adds a field to an embed message.")
				.addOptions(
						new OptionData(OptionType.STRING, "message-id", "The embed's message id.", true),
						new OptionData(OptionType.INTEGER, "field-position", "The field's position. Starts with 0.", true)
								.setMaxValue(24),
						new OptionData(OptionType.CHANNEL, "channel", "What channel is the embed in?", false)
								.setChannelTypes(ChannelType.TEXT, ChannelType.VOICE, ChannelType.GUILD_PRIVATE_THREAD, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_NEWS_THREAD)
				)
		);
	}

	@Override
	protected void handleEmbedSubcommand(@NotNull SlashCommandInteractionEvent event, long messageId, GuildMessageChannel channel) {
		OptionMapping positionMapping = event.getOption("field-position");
		if (positionMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		int position = positionMapping.getAsInt();
		event.deferReply(true).queue();
		channel.retrieveMessageById(messageId).queue(message -> {
			// just get the first embed
			if (message.getEmbeds().isEmpty()) {
				Responses.error(event.getHook(), "The provided message does not have any embeds attached. Please try again.").queue();
				return;
			}
			EmbedBuilder builder = new EmbedBuilder(message.getEmbeds().get(0));
			if (position > builder.getFields().size()) {
				Responses.error(event.getHook(), "The provided position does not exist; the embed does not have that many fields.").queue();
				return;
			}
			builder.getFields().remove(position);
			message.editMessageEmbeds(builder.build()).queue();
			Responses.info(event.getHook(), "Embed Field Removed", "Successfully removed field `%s` from %s", position, message.getJumpUrl()).queue();
		});
	}
}
