package net.discordjug.javabot.listener;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.WebhookUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BlacklistedMessageAttachmentListener extends ListenerAdapter {

    private final BotConfig botConfig;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
        GuildConfig guildConfig = botConfig.get(event.getGuild());
        List<String> blacklistedMessageExtensions = guildConfig.getBlacklistedMessageExtensions();
        Message message = event.getMessage();
        List<Message.Attachment> attachments = message.getAttachments();
        List<Message.Attachment> allowedAttachments = new ArrayList<>();
        attachments.forEach(attachment -> {
            boolean contains = blacklistedMessageExtensions.contains(attachment.getContentType());
            if (!contains) {
                allowedAttachments.add(attachment);
            }
        });
        if (message.getAttachments().size() != allowedAttachments.size()) {
            IWebhookContainer tc = null;
            if (event.isFromType(ChannelType.TEXT)) {
                tc = event.getChannel().asTextChannel();
            }
            if (event.isFromThread()) {
                StandardGuildChannel parentChannel = event.getChannel()
                        .asThreadChannel()
                        .getParentChannel()
                        .asStandardGuildChannel();
                tc = (IWebhookContainer) parentChannel;
            }
            if (tc == null) {
                return;
            }
            long threadId = event.isFromThread() ? event.getChannel().getIdLong() : 0;
            WebhookUtil.ensureWebhookExists(
                    tc,
                    wh -> WebhookUtil.replaceMemberMessageWithAttachments(
                            wh,
                            event.getMessage(),
                            event.getMessage().getContentRaw(),
                            threadId,
                            allowedAttachments
                    ),
                    e -> ExceptionLogger.capture(
                            e,
                            "Error creating webhook for UnformattedCodeListener"
                    )
            );
        }
    }
}
