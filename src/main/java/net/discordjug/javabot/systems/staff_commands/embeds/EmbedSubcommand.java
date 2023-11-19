package net.discordjug.javabot.systems.staff_commands.embeds;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract parent class for all edit-embed subcommands.
 */
@RequiredArgsConstructor
public abstract class EmbedSubcommand extends SlashCommand.Subcommand {
	private final BotConfig botConfig;

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("message-id");
		if (idMapping == null || Checks.isInvalidLongInput(idMapping)) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
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
		handleEmbedSubcommand(event, idMapping.getAsLong(), channel);
	}

	protected abstract void handleEmbedSubcommand(SlashCommandInteractionEvent event, long messageId, GuildMessageChannel channel);
}
