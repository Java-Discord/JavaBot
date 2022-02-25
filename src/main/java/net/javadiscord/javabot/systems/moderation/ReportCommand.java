package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.interfaces.IMessageContextCommand;
import net.javadiscord.javabot.command.interfaces.IUserContextCommand;
import net.javadiscord.javabot.command.moderation.ModerateUserCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;

import java.time.Instant;

/**
 * Command that allows members to report other members.
 */
public class ReportCommand extends ModerateUserCommand implements IUserContextCommand, IMessageContextCommand {
	@Override
	public ReplyCallbackAction handleMessageContextCommandInteraction(MessageContextInteractionEvent event) throws ResponseException {
		var config = Bot.config.get(event.getGuild());
		var embed = buildReportEmbed(event.getTarget().getAuthor(), event.getUser(), event.getTextChannel(), config.getSlashCommand());
		embed.addField("Message", String.format("[Jump to Message](%s)", event.getTarget().getJumpUrl()), false);
		MessageChannel reportChannel = config.getModeration().getReportChannel();
		reportChannel.sendMessage("@here").setEmbeds(embed.build())
				.setActionRows(setComponents(event.getTarget().getAuthor().getIdLong()))
				.queue();
		embed.setDescription("Successfully reported " + "`" + event.getTarget().getAuthor().getAsTag() + "`!\nYour report has been send to our Moderators");
		return event.replyEmbeds(embed.build()).setEphemeral(true);
	}

	@Override
	public ReplyCallbackAction handleUserContextCommandInteraction(UserContextInteractionEvent event) throws ResponseException {
		var config = Bot.config.get(event.getGuild());
		var embed = buildReportEmbed(event.getTarget(), event.getUser(), event.getTextChannel(), config.getSlashCommand());
		MessageChannel reportChannel = config.getModeration().getReportChannel();
		reportChannel.sendMessage("@here").setEmbeds(embed.build())
				.setActionRows(setComponents(event.getTarget().getIdLong()))
				.queue();
		embed.setDescription("Successfully reported " + "`" + event.getTarget().getAsTag() + "`!\nYour report has been send to our Moderators");
		return event.replyEmbeds(embed.build()).setEphemeral(true);
	}

	private ActionRow setComponents(long userId) {
		return ActionRow.of(
				Button.danger("utils:ban:" + userId, "Ban"),
				Button.danger("utils:kick:" + userId, "Kick"),
				Button.secondary("utils:delete", "🗑️")
		);
	}

	private EmbedBuilder buildReportEmbed(User reported, User reportedBy, TextChannel channel, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(reported.getAsTag(), null, reported.getEffectiveAvatarUrl())
				.setTitle("Report")
				.setColor(config.getDefaultColor())
				.addField("Member", reported.getAsMention(), true)
				.addField("Reported by", reportedBy.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Reported on", String.format("<t:%s:F>", Instant.now().getEpochSecond()), false)
				.addField("ID", String.format("```%s```", reported.getId()), true)
				.setFooter(reportedBy.getAsTag(), reportedBy.getEffectiveAvatarUrl())
				.setTimestamp(Instant.now());
	}

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(SlashCommandInteractionEvent event, Member commandUser, Member target) throws ResponseException {
		OptionMapping option = event.getOption("reason");
		String reason = option == null ? "None" : option.getAsString();

		var config = Bot.config.get(event.getGuild());
		MessageChannel reportChannel = config.getModeration().getReportChannel();
		var embed = buildReportEmbed(target.getUser(), commandUser.getUser(), event.getTextChannel(), config.getSlashCommand());
		embed.addField("Reason", String.format("```%s```", reason), false);
		reportChannel.sendMessage("@here").setEmbeds(embed.build())
				.setActionRows(setComponents(target.getIdLong()))
				.queue();
		embed.setDescription("Successfully reported " + "`" + target.getUser().getAsTag() + "`!\nYour report has been send to our Moderators");
		return event.replyEmbeds(embed.build()).setEphemeral(true);
	}
}


