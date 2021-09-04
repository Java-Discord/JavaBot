package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * This command deletes messages from a channel.
 */
public class Purge implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return Responses.warning(event, "This command can only be used in a guild.");
        }
        if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
            return Responses.warning(event, "You do not have the `MESSAGE_MANAGE` permission which is required to remove messages.");
        }

        OptionMapping amountOption = event.getOption("amount");
        OptionMapping userOption = event.getOption("user");
        OptionMapping archiveOption = event.getOption("archive");

        Long amount = (amountOption == null) ? null : amountOption.getAsLong();
        User user = (userOption == null) ? null : userOption.getAsUser();
        boolean archive = archiveOption != null && archiveOption.getAsBoolean();

        if (amount != null && (amount < 1 || amount > 1_000_000)) {
            return Responses.warning(event, "Invalid amount. If specified, should be between 1 and 1,000,000, inclusive.");
        }
        /*
        int amount = (int) event.getOption("amount").getAsLong();
        boolean nuke;
        try {
            nuke = event.getOption("nuke-channel").getAsBoolean();
        } catch (NullPointerException e) {
            nuke = false;
        }

            try {
                if (nuke) {
                    event.getTextChannel().createCopy().queue();
                    event.getTextChannel().delete().queue();
                }

                MessageHistory history = new MessageHistory(event.getChannel());
                List<Message> messages = history.retrievePast(amount + 1).complete();

                //for (int i = messages.size() - 1; i > 0; i--) messages.get(i).delete().queue();
                event.getTextChannel().deleteMessages(messages).complete();

                var e = new EmbedBuilder()
                    .setColor(new Color(0x2F3136))
                    .setTitle("Successfully deleted **" + amount + " messages** :broom:")
                    .build();

                return event.replyEmbeds(e);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                return event.replyEmbeds(Embeds.purgeError(event)).setEphemeral(Constants.ERR_EPHEMERAL);
            }

         */
        Bot.asyncPool.submit(() -> this.purge(amount, user, archive, event.getTextChannel()));
        StringBuilder sb = new StringBuilder();
        sb.append(amount != null ? (amount > 1 ? amount + " messages " : "1 message ") : "All messages ");
        if (user != null) {
            sb.append("by the user ").append(user.getAsTag()).append(' ');
        }
        sb.append("will be removed").append(archive ? " and placed in an archive." : '.');
        return Responses.info(event, "Purge Started", sb.toString());
    }

    private void purge(@Nullable Long amount, @Nullable User user, boolean archive, TextChannel channel) {
        var history = channel.getHistory();
        List<Message> messages;
        long count = 0;
        do {
            messages = history.retrievePast(amount == null ? 100 : (int) Math.min(100, amount)).complete();
            if (messages.isEmpty()) return;
        } while ((amount != null && amount > count) || (amount == null && !messages.isEmpty()));
    }
}