package net.javadiscord.javabot.systems.help;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.moderation.AutoMod;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.WebhookUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Handles auto-formatting of code in help-channels.
 * A non-listener class since it has to be called strictly after {@link HelpListener} to not send its messages before channel reserving.
 */
@AllArgsConstructor
public class AutoCodeFormatter {
	private final AutoMod autoMod;
	private final BotConfig botConfig;
	private final UserPreferenceService preferenceService;

	/**
	 * Method responsible for finding a codeblock, if present.
	 *
	 * @param event a {@link MessageReceivedEvent}.
	 * @return a MessageCodeblock instance, holding a startIndex, content and an endIndex
	 */
	@Nullable
	private static AutoCodeFormatter.CodeBlock findCodeblock(@NotNull MessageReceivedEvent event) {
		String msg = event.getMessage().getContentRaw();
		int openingBracket = msg.indexOf("{");
		int closingBracket = msg.lastIndexOf("}");
		if (closingBracket == -1 || openingBracket == -1) return null;
		int startIndex = msg.lastIndexOf("\n", openingBracket);
		int endIndex = msg.indexOf("\n", closingBracket);
		if (startIndex == -1) startIndex = 0;
		if (endIndex == -1) endIndex = msg.length();
		return new CodeBlock(startIndex, endIndex);
	}

	/**
	 * called by {@link HelpListener#onMessageReceived(MessageReceivedEvent)} on every message.
	 * It is worth noting that this class can't register as an event handler itself due to there being no way of
	 * setting its methods to be strictly called after {@link HelpListener}.
	 *
	 * @param event a {@link MessageReceivedEvent}
	 */
	public void handleMessageEvent(@Nonnull MessageReceivedEvent event) {
		if (!event.isFromGuild()) return;
		if (event.getAuthor().isBot() || event.getMessage().getAuthor().isSystem()) return;
		if (autoMod.hasSuspiciousLink(event.getMessage()) || autoMod.hasAdvertisingLink(event.getMessage())) return;
		if (event.isWebhookMessage()) return;
		if (!event.isFromThread()) return;
		if (event.getChannel().asThreadChannel().getParentChannel().getIdLong() != botConfig.get(event.getGuild()).getHelpConfig().getHelpForumChannelId()) {
			return;
		}
		if (!Boolean.parseBoolean(preferenceService.getOrCreate(Objects.requireNonNull(event.getMember()).getIdLong(), Preference.FORMAT_UNFORMATTED_CODE).getState())) {
			return;
		}


		if (event.getMessage().getContentRaw().contains("```")) return; // exit if already contains codeblock

		CodeBlock code = findCodeblock(event);
		if (code == null) return;

		if (event.getMessage().getMentions().getUsers().isEmpty() && event.getChannel().asThreadChannel().getTotalMessageCount() > 1) {
			replaceUnformattedCode(event.getMessage().getContentRaw(), code.startIndex(), code.endIndex(), event);
		} else sendFormatHint(event);
	}

	private void sendFormatHint(MessageReceivedEvent event) {
		event.getChannel().sendMessageEmbeds(formatHintEmbed(event.getGuild())).queue();
	}

	private void replaceUnformattedCode(String msg, int codeStartIndex, int codeEndIndex, MessageReceivedEvent event) {
		// default case: a "normal", non-ping containing, non first message of a forum-thread containing "{" and "}".
		// user must also have set their preferences to allow this.
		if (event.getMessage().getContentRaw().length() > 1992) { // can't exceed discord's char limit
			sendFormatHint(event);
			return;
		}
		String messageContent = msg.substring(0, codeStartIndex) + " ```" + msg.substring(codeStartIndex, codeEndIndex) + " ```" + msg.substring(codeEndIndex);
		EmbedBuilder autoformatInfo = new EmbedBuilder().setDescription(botConfig.get(event.getGuild()).getHelpConfig().getAutoformatInfoMessage());
		WebhookUtil.ensureWebhookExists(event.getChannel().asThreadChannel().getParentChannel().asForumChannel(), wh -> WebhookUtil.replaceMemberMessage(wh, event.getMessage(), messageContent, event.getChannel().getIdLong(), autoformatInfo.build(), formatHintEmbed(event.getGuild())), e -> ExceptionLogger.capture(e, "Error creating webhook for UnformattedCodeListener"));
	}

	private MessageEmbed formatHintEmbed(Guild guild) {
		return new EmbedBuilder().setDescription(botConfig.get(guild).getHelpConfig().getFormatHintMessage()).build();
	}

	private record CodeBlock(int startIndex, int endIndex) {
	}
}
