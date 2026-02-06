package net.discordjug.javabot.listener.filter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.PatternTypeAdapter;
import net.discordjug.javabot.data.config.guild.MessageRule;
import net.discordjug.javabot.data.config.guild.MessageRule.MessageAction;
import net.discordjug.javabot.data.config.guild.ModerationConfig;
import net.discordjug.javabot.data.h2db.message_cache.MessageCache;
import net.discordjug.javabot.data.h2db.message_cache.model.CachedMessage;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.stereotype.Component;

/**
 * This {@link MessageFilter} acts on messages according to {@link MessageRule}s.
 * If a message rule matches, the corresponding action is executed.
 */
@Component
@RequiredArgsConstructor
public class MessageRuleFilter implements MessageFilter {

	private final BotConfig botConfig;
	private final MessageCache messageCache;

	@Override
	public MessageModificationStatus processMessage(MessageContent content) {

		ModerationConfig moderationConfig = botConfig.get(content.event().getGuild()).getModerationConfig();
		List<MessageRule> messageRules = moderationConfig.getMessageRules();

		MessageRule ruleToExecute = null;
		for (MessageRule rule : messageRules) {
			if (matches(content, rule)) {
				if (ruleToExecute == null || rule.getAction() == MessageAction.BLOCK) {
					ruleToExecute = rule;
				}
			}
		}
		MessageModificationStatus status = MessageModificationStatus.NOT_MODIFIED;
		if (ruleToExecute != null) {
			if (ruleToExecute.getAction() == MessageAction.BLOCK && !Checks.hasStaffRole(botConfig, content.event().getMember())) {
				content.event().getMessage().delete()
					.flatMap(_ -> content.event().getChannel().sendMessage(content.event().getAuthor().getAsMention() + " Your message has been deleted for moderative reasons. If you believe this happened by mistake, please contact the server staff."))
					.delay(Duration.ofSeconds(60))
					.flatMap(Message::delete)
					.queue();
				status = MessageModificationStatus.STOP_PROCESSING;
			}
			log(content, ruleToExecute, moderationConfig);
		}

		return status;
	}

	private void log(MessageContent content, MessageRule ruleToExecute, ModerationConfig moderationConfig) {
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.setPrettyPrinting()
				.registerTypeAdapter(Pattern.class, new PatternTypeAdapter())
				.create();
		EmbedBuilder embed = messageCache.buildMessageCacheEmbed(
				content.event().getMessage().getChannel(),
				content.event().getMessage().getAuthor(),
				CachedMessage.of(content.event().getMessage()), "Message content")
			.setTitle("Message rule triggered")
			.addField("Rule description", "```\n" + gson.toJson(ruleToExecute) + "\n```", false);
		if (!content.attachments().isEmpty()) {
			embed.addField("Attachment hashes", computeAttachmentDescription(content.attachments()), false);
		}
		moderationConfig.getLogChannel().sendMessageEmbeds(embed.build()).queue();
	}

	private boolean matches(MessageContent content, MessageRule rule) {
		if (rule.getMessageRegex() != null && !rule.getMessageRegex().matcher(content.messageText()).matches()) {
			return false;
		}
		if (content.attachments().size() > rule.getMaxAttachments()) {
			return false;
		}
		if (content.attachments().size() < rule.getMinAttachments()) {
			return false;
		}
		boolean matchesSHA = rule.getAttachmentSHAs().isEmpty();
		for (Attachment attachment : content.attachments()) {
			if (rule.getAttachmentNameRegex() != null && !rule.getAttachmentNameRegex().matcher(attachment.getFileName()).matches()) {
				return false;
			}
			if (!matchesSHA) {
				if (rule.getAttachmentSHAs().contains(computeSHA(attachment))) {
					matchesSHA = true;
				}
			}
		}
		return matchesSHA;
	}

	private String computeAttachmentDescription(List<Message.Attachment> attachments) {
		return attachments.stream()
				.map(attachment -> "- " + attachment.getUrl() + ": `" + computeSHA(attachment) + "`")
				.collect(Collectors.joining("\n"));
	}
	
	private String computeSHA(Attachment attachment) {
		try {
			HttpResponse<byte[]> res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(attachment.getProxyUrl())).build(), BodyHandlers.ofByteArray());
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(res.body());
			return Base64.getEncoder().encodeToString(hash);
		} catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
			ExceptionLogger.capture(e);
			return "";
		}
	}
}
