package net.javadiscord.javabot.listener;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.internal.entities.WebhookImpl;
import net.dv8tion.jda.internal.requests.restaction.WebhookMessageActionImpl;
import net.javadiscord.javabot.Bot;

/**
 * Replaces all occurences of 'fuck' in incoming messages with 'hug'.
 */
public class HugListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
		if(!event.isFromGuild()) {
			return;
		}
		if (Bot.autoMod.hasSuspiciousLink(event.getMessage()) || Bot.autoMod.hasAdvertisingLink(event.getMessage())) {
			return;
		}
		TextChannel tc=event.getTextChannel();
		if(event.isFromThread()) {
			GuildMessageChannel parentChannel = event.getThreadChannel().getParentMessageChannel();
			if(parentChannel instanceof TextChannel textChannel) {
				tc=textChannel;
			}
		}
		if(tc==null) {
			return;
		}
		final TextChannel textChannel=tc;
		String content=event.getMessage().getContentRaw();
		String lowerCaseContent=content.toLowerCase();
		if(lowerCaseContent.contains("fuck")) {
			StringBuilder sb=new StringBuilder(content.length());
			int index=0;
			int indexBkp=index;
			while((index=lowerCaseContent.indexOf("fuck",index))!=-1) {
				sb.append(content.substring(indexBkp,index));
				sb.append("hug");
				indexBkp=index+++4;
			}
			
			sb.append(content.substring(indexBkp,content.length()));
			event.getMessage().delete().queue(unused->{
				textChannel.retrieveWebhooks().queue(webhooks->{
					Optional<Webhook> hook = webhooks
					.stream()
					.filter(webhook->webhook.getChannel().getIdLong()==event.getChannel().getIdLong())
					.filter(wh->wh.getToken()!=null)
					.findAny();
					if(hook.isPresent()) {
						buildMessage(hook.get(), event.getMessage(), sb.toString(),(BaseGuildMessageChannel) event.getGuildChannel());
					}else {
						textChannel
							.createWebhook("JavaBot-hug")
							.queue(wh->
								buildMessage(wh, event.getMessage(), sb.toString(),(BaseGuildMessageChannel) event.getGuildChannel())
							);
					}
				});
			});
		}
	}
	
	private void buildMessage(Webhook webhook, Message originalMessage, String newMessageContent, BaseGuildMessageChannel channel){
		WebhookImpl webhookImpl = new WebhookImpl(channel, webhook.getJDA(), webhook.getIdLong(), WebhookType.INCOMING);
		webhookImpl.setToken(webhook.getToken());
		WebhookMessageActionImpl<Void> ret = webhookImpl
				.sendMessage(newMessageContent)
				.allowedMentions(Collections.emptyList());
		
		List<Attachment> attachments = originalMessage.getAttachments();
		@SuppressWarnings("unchecked")
		CompletableFuture<?>[] futures=new CompletableFuture<?>[attachments.size()];
		
		for(int i = 0; i < attachments.size(); i++){
			Attachment attachment = attachments.get(i);
			futures[i]=attachment
					.getProxy()
					.download()
					.thenAccept(is->
						ret.addFile(
								is,
								attachment.getFileName(),
								attachment.isSpoiler() ? new AttachmentOption[] { AttachmentOption.SPOILER } : new AttachmentOption[0]
						)
					);
		}
		CompletableFuture.allOf(futures)
			.thenAccept(unused->ret.queue());
	}
}
