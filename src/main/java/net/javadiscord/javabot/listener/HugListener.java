package net.javadiscord.javabot.listener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;

/**
 * Replaces all occurences of 'fuck' in incoming messages with 'hug'.
 */
@Slf4j
public class HugListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
		if(!event.isFromGuild()) {
			return;
		}
		if (Bot.autoMod.hasSuspiciousLink(event.getMessage()) || Bot.autoMod.hasAdvertisingLink(event.getMessage())) {
			return;
		}
		TextChannel tc = null;
		if(event.isFromType(ChannelType.TEXT)) {
			tc = event.getTextChannel();
		}
		if(event.isFromThread()) {
			GuildMessageChannel parentChannel = event.getThreadChannel().getParentMessageChannel();
			if(parentChannel instanceof TextChannel textChannel) {
				tc = textChannel;
			}
		}
		if(tc == null) {
			return;
		}
		final TextChannel textChannel = tc;
		String content = event.getMessage().getContentRaw();
		String lowerCaseContent = content.toLowerCase();
		if(lowerCaseContent.contains("fuck")) {
			long threadId = event.isFromThread()?event.getThreadChannel().getIdLong():0;
			StringBuilder sb = new StringBuilder(content.length());
			int index = 0;
			int indexBkp = index;
			while((index = lowerCaseContent.indexOf("fuck",index)) != -1) {
				sb.append(content.substring(indexBkp,index));
				sb.append("hug");
				indexBkp=index++ +4;
			}
			
			sb.append(content.substring(indexBkp,content.length()));
			textChannel.retrieveWebhooks().queue(webhooks->{
				Optional<Webhook> hook = webhooks
				.stream()
				.filter(webhook->webhook.getChannel().getIdLong() == textChannel.getIdLong())
				.filter(wh->wh.getToken()!=null)
				.findAny();
				if(hook.isPresent()) {
					sendWebhookMessage(hook.get(), event.getMessage(), sb.toString(),threadId);
				}else {
					textChannel
						.createWebhook("JavaBot-hug")
						.queue(wh->
							sendWebhookMessage(wh, event.getMessage(), sb.toString(), threadId)
						);
				}
			});
		}
	}
	
	private void sendWebhookMessage(Webhook webhook, Message originalMessage, String newMessageContent, long threadId){
		JDAWebhookClient client = new WebhookClientBuilder(webhook.getIdLong(), webhook.getToken())
				.setThreadId(threadId)
				.buildJDA();
		WebhookMessageBuilder message = new WebhookMessageBuilder()
				.setContent(newMessageContent)
				.setAllowedMentions(AllowedMentions.none())
				.setAvatarUrl(originalMessage.getMember().getEffectiveAvatarUrl())
				.setUsername(originalMessage.getMember().getEffectiveName());
		
		List<Attachment> attachments = originalMessage.getAttachments();
		@SuppressWarnings("unchecked")
		CompletableFuture<?>[] futures = new CompletableFuture<?>[attachments.size()];
		for(int i = 0; i < attachments.size(); i++){
			Attachment attachment = attachments.get(i);
			futures[i] = attachment
					.getProxy()
					.download()
					.thenAccept(is ->
						message.addFile(
								(attachment.isSpoiler()?"SPOILER_":"")+attachment.getFileName(),
								is
						)
					);
		}
		CompletableFuture.allOf(futures)
			.thenAccept(unused -> client.send(message.build()))
			.thenAccept(unused -> originalMessage.delete().queue())
			.exceptionally(e ->{
				log.error("replacing the content 'fuck' with 'hug' in an incoming message failed", e);
				return null;
			});
	}
}
